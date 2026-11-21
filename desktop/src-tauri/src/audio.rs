//! Playback engine: symphonia decodes, rubato resamples to the device rate,
//! cpal (WASAPI shared mode on Windows) plays from a lock-free ring buffer.
//!
//! One thread owns the decoder and the output stream. The UI talks to it through
//! a command channel and hears back through Tauri events.

use std::fs::File;
use std::path::Path;
use std::sync::atomic::{AtomicBool, AtomicU32, AtomicU64, Ordering};
use std::sync::Arc;
use std::thread;
use std::time::{Duration, Instant};

use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use cpal::{FromSample, SizedSample};
use crossbeam_channel::{unbounded, Receiver, RecvTimeoutError, Sender};
use ringbuf::traits::{Consumer, Observer, Producer, Split};
use ringbuf::{HeapCons, HeapProd, HeapRb};
use rubato::{FftFixedIn, Resampler};
use serde::Serialize;
use symphonia::core::audio::SampleBuffer;
use symphonia::core::codecs::{Decoder, DecoderOptions, CODEC_TYPE_NULL};
use symphonia::core::errors::Error as SymError;
use symphonia::core::formats::{FormatOptions, FormatReader, SeekMode, SeekTo};
use symphonia::core::io::MediaSourceStream;
use symphonia::core::meta::MetadataOptions;
use symphonia::core::probe::Hint;
use symphonia::core::units::Time;
use tauri::{AppHandle, Emitter};

const RESAMPLE_CHUNK: usize = 1024;

pub enum Cmd {
    Load { path: String, autoplay: bool },
    Play,
    Pause,
    Seek(f64),
    Volume(f32),
    SetDevice(Option<String>),
    Stop,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PlayerState {
    pub path: Option<String>,
    pub playing: bool,
    pub position: f64,
    pub duration: f64,
}

#[derive(Clone, Serialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct OutputInfo {
    pub device: String,
    pub sample_rate: u32,
    pub channels: u16,
    pub source_rate: u32,
    pub resampling: bool,
    pub mode: String,
}

/// State the output callback and the engine thread both touch.
struct Shared {
    playing: AtomicBool,
    /// Volume gain stored as f32 bits.
    gain: AtomicU32,
    /// Set by the engine to ask the callback to drop everything buffered.
    flush: AtomicBool,
    /// Frames the callback has actually played since the last flush.
    frames_played: AtomicU64,
    /// The output device failed (unplugged DAC, driver reset).
    device_error: AtomicBool,
}

pub struct Player {
    tx: Sender<Cmd>,
    output: Arc<std::sync::Mutex<OutputInfo>>,
}

impl Player {
    pub fn spawn(app: AppHandle) -> Self {
        let (tx, rx) = unbounded();
        let output = Arc::new(std::sync::Mutex::new(OutputInfo::default()));
        let out2 = output.clone();
        thread::Builder::new()
            .name("audio-engine".into())
            .spawn(move || Engine::new(app, rx, out2).run())
            .expect("spawn audio thread");
        Self { tx, output }
    }

    pub fn send(&self, cmd: Cmd) {
        let _ = self.tx.send(cmd);
    }

    pub fn output_info(&self) -> OutputInfo {
        self.output.lock().map(|o| o.clone()).unwrap_or_default()
    }
}

pub fn list_output_devices() -> Vec<String> {
    let host = cpal::default_host();
    host.output_devices()
        .map(|it| it.filter_map(|d| d.name().ok()).collect())
        .unwrap_or_default()
}

// ---------------------------------------------------------------------------
// Output stream
// ---------------------------------------------------------------------------

struct Output {
    _stream: cpal::Stream,
    prod: HeapProd<f32>,
    rate: u32,
    channels: u16,
    name: String,
}

fn open_output(shared: &Arc<Shared>, wanted: Option<&str>) -> Result<Output, String> {
    let host = cpal::default_host();
    let device = match wanted {
        Some(name) => host
            .output_devices()
            .map_err(|e| e.to_string())?
            .find(|d| d.name().map(|n| n == name).unwrap_or(false))
            .or_else(|| host.default_output_device()),
        None => host.default_output_device(),
    }
    .ok_or("No audio output device found")?;
    let name = device.name().unwrap_or_else(|_| "Audio output".into());
    let supported = device.default_output_config().map_err(|e| e.to_string())?;
    let rate = supported.sample_rate().0;
    let channels = supported.channels();
    let config: cpal::StreamConfig = supported.config();

    // Half a second of stereo audio at the device rate.
    let rb = HeapRb::<f32>::new(rate as usize);
    let (prod, cons) = rb.split();
    shared.flush.store(false, Ordering::SeqCst);
    shared.frames_played.store(0, Ordering::SeqCst);
    shared.device_error.store(false, Ordering::SeqCst);

    let stream = match supported.sample_format() {
        cpal::SampleFormat::F32 => build_stream::<f32>(&device, &config, cons, shared.clone()),
        cpal::SampleFormat::I16 => build_stream::<i16>(&device, &config, cons, shared.clone()),
        cpal::SampleFormat::U16 => build_stream::<u16>(&device, &config, cons, shared.clone()),
        cpal::SampleFormat::I32 => build_stream::<i32>(&device, &config, cons, shared.clone()),
        other => return Err(format!("Unsupported output sample format {other:?}")),
    }?;
    stream.play().map_err(|e| e.to_string())?;
    Ok(Output { _stream: stream, prod, rate, channels, name })
}

fn build_stream<T>(
    device: &cpal::Device,
    config: &cpal::StreamConfig,
    mut cons: HeapCons<f32>,
    shared: Arc<Shared>,
) -> Result<cpal::Stream, String>
where
    T: SizedSample + FromSample<f32>,
{
    let ch = config.channels as usize;
    let err_shared = shared.clone();
    device
        .build_output_stream(
            config,
            move |data: &mut [T], _| {
                if shared.flush.load(Ordering::Acquire) {
                    cons.clear();
                    shared.frames_played.store(0, Ordering::Release);
                    shared.flush.store(false, Ordering::Release);
                }
                let playing = shared.playing.load(Ordering::Relaxed);
                let gain = f32::from_bits(shared.gain.load(Ordering::Relaxed));
                let mut played = 0u64;
                for frame in data.chunks_mut(ch) {
                    let (l, r) = if playing && cons.occupied_len() >= 2 {
                        played += 1;
                        (cons.try_pop().unwrap_or(0.0), cons.try_pop().unwrap_or(0.0))
                    } else {
                        (0.0, 0.0)
                    };
                    for (i, s) in frame.iter_mut().enumerate() {
                        let v = match i {
                            0 => l,
                            1 => r,
                            _ => 0.0,
                        };
                        *s = T::from_sample(v * gain);
                    }
                    if ch == 1 {
                        frame[0] = T::from_sample((l + r) * 0.5 * gain);
                    }
                }
                shared.frames_played.fetch_add(played, Ordering::Relaxed);
            },
            move |err| {
                log::error!("audio output error: {err}");
                err_shared.device_error.store(true, Ordering::SeqCst);
            },
            None,
        )
        .map_err(|e| e.to_string())
}

// ---------------------------------------------------------------------------
// Decoder
// ---------------------------------------------------------------------------

struct Source {
    path: String,
    format: Box<dyn FormatReader>,
    decoder: Box<dyn Decoder>,
    track_id: u32,
    src_rate: u32,
    duration: f64,
    resampler: Option<FftFixedIn<f32>>,
    pending: [Vec<f32>; 2],
    /// Stream position (seconds) that frame 0 of the ring buffer corresponds to.
    base: f64,
    eof: bool,
}

impl Source {
    fn open(path: &str, dst_rate: u32) -> Result<Self, String> {
        let file = File::open(path).map_err(|e| format!("Cannot open file: {e}"))?;
        let mss = MediaSourceStream::new(Box::new(file), Default::default());
        let mut hint = Hint::new();
        if let Some(ext) = Path::new(path).extension().and_then(|e| e.to_str()) {
            hint.with_extension(ext);
        }
        let probed = symphonia::default::get_probe()
            .format(&hint, mss, &FormatOptions { enable_gapless: true, ..Default::default() }, &MetadataOptions::default())
            .map_err(|e| format!("Unsupported file: {e}"))?;
        let format = probed.format;
        let track = format
            .tracks()
            .iter()
            .find(|t| t.codec_params.codec != CODEC_TYPE_NULL)
            .ok_or("No audio track in file")?;
        let track_id = track.id;
        let params = track.codec_params.clone();
        let src_rate = params.sample_rate.ok_or("Unknown sample rate")?;
        let duration = match (params.n_frames, params.time_base) {
            (Some(n), Some(tb)) => {
                let t = tb.calc_time(n);
                t.seconds as f64 + t.frac
            }
            (Some(n), None) => n as f64 / src_rate as f64,
            _ => 0.0,
        };
        let decoder = symphonia::default::get_codecs()
            .make(&params, &DecoderOptions::default())
            .map_err(|e| format!("Unsupported codec: {e}"))?;
        let mut src = Self {
            path: path.to_string(),
            format,
            decoder,
            track_id,
            src_rate,
            duration,
            resampler: None,
            pending: [Vec::new(), Vec::new()],
            base: 0.0,
            eof: false,
        };
        src.set_output_rate(dst_rate)?;
        Ok(src)
    }

    fn set_output_rate(&mut self, dst_rate: u32) -> Result<(), String> {
        self.resampler = if dst_rate == self.src_rate {
            None
        } else {
            Some(
                FftFixedIn::<f32>::new(self.src_rate as usize, dst_rate as usize, RESAMPLE_CHUNK, 2, 2)
                    .map_err(|e| e.to_string())?,
            )
        };
        self.pending = [Vec::new(), Vec::new()];
        Ok(())
    }

    fn seek(&mut self, secs: f64) -> f64 {
        let secs = secs.clamp(0.0, (self.duration - 0.25).max(0.0));
        let to = SeekTo::Time { time: Time::new(secs.trunc() as u64, secs.fract()), track_id: Some(self.track_id) };
        let landed = match self.format.seek(SeekMode::Accurate, to) {
            Ok(s) => self
                .format
                .tracks()
                .iter()
                .find(|t| t.id == self.track_id)
                .and_then(|t| t.codec_params.time_base)
                .map(|tb| {
                    let t = tb.calc_time(s.actual_ts);
                    t.seconds as f64 + t.frac
                })
                .unwrap_or(secs),
            Err(e) => {
                log::warn!("seek failed: {e}");
                secs
            }
        };
        self.decoder.reset();
        if let Some(r) = self.resampler.as_mut() {
            r.reset();
        }
        self.pending = [Vec::new(), Vec::new()];
        self.eof = false;
        self.base = landed;
        landed
    }

    /// Decodes one packet and appends interleaved stereo samples at the
    /// output rate to `out`. Sets `eof` when the file is finished.
    fn decode_into(&mut self, out: &mut Vec<f32>) -> Result<(), String> {
        loop {
            let packet = match self.format.next_packet() {
                Ok(p) => p,
                Err(SymError::IoError(e)) if e.kind() == std::io::ErrorKind::UnexpectedEof => {
                    self.finish(out)?;
                    return Ok(());
                }
                Err(SymError::ResetRequired) => {
                    self.decoder.reset();
                    continue;
                }
                Err(e) => {
                    log::warn!("read error, ending track: {e}");
                    self.finish(out)?;
                    return Ok(());
                }
            };
            if packet.track_id() != self.track_id {
                continue;
            }
            let decoded = match self.decoder.decode(&packet) {
                Ok(d) => d,
                Err(SymError::DecodeError(e)) => {
                    log::warn!("skipping bad packet: {e}");
                    continue;
                }
                Err(e) => return Err(e.to_string()),
            };
            let spec = *decoded.spec();
            let frames = decoded.frames();
            if frames == 0 {
                continue;
            }
            let mut buf = SampleBuffer::<f32>::new(frames as u64, spec);
            buf.copy_interleaved_ref(decoded);
            let ch = spec.channels.count().max(1);
            let samples = buf.samples();
            match self.resampler.is_some() {
                false => {
                    for f in samples.chunks(ch) {
                        let l = f[0];
                        let r = if ch > 1 { f[1] } else { l };
                        out.push(l);
                        out.push(r);
                    }
                }
                true => {
                    for f in samples.chunks(ch) {
                        self.pending[0].push(f[0]);
                        self.pending[1].push(if ch > 1 { f[1] } else { f[0] });
                    }
                    self.drain_resampler(out, false)?;
                }
            }
            return Ok(());
        }
    }

    fn drain_resampler(&mut self, out: &mut Vec<f32>, flush: bool) -> Result<(), String> {
        let Some(rs) = self.resampler.as_mut() else { return Ok(()) };
        loop {
            let need = rs.input_frames_next();
            if self.pending[0].len() >= need {
                let chunk = [&self.pending[0][..need], &self.pending[1][..need]];
                let res = rs.process(&chunk, None).map_err(|e| e.to_string())?;
                interleave(&res, out);
                self.pending[0].drain(..need);
                self.pending[1].drain(..need);
            } else if flush && !self.pending[0].is_empty() {
                let chunk = [&self.pending[0][..], &self.pending[1][..]];
                let res = rs.process_partial(Some(&chunk), None).map_err(|e| e.to_string())?;
                interleave(&res, out);
                self.pending = [Vec::new(), Vec::new()];
            } else {
                return Ok(());
            }
        }
    }

    fn finish(&mut self, out: &mut Vec<f32>) -> Result<(), String> {
        self.drain_resampler(out, true)?;
        self.eof = true;
        Ok(())
    }
}

fn interleave(planar: &[Vec<f32>], out: &mut Vec<f32>) {
    if planar.len() < 2 {
        return;
    }
    for (l, r) in planar[0].iter().zip(planar[1].iter()) {
        out.push(*l);
        out.push(*r);
    }
}

// ---------------------------------------------------------------------------
// Engine thread
// ---------------------------------------------------------------------------

struct Engine {
    app: AppHandle,
    rx: Receiver<Cmd>,
    shared: Arc<Shared>,
    output: Option<Output>,
    output_info: Arc<std::sync::Mutex<OutputInfo>>,
    wanted_device: Option<String>,
    source: Option<Source>,
    scratch: Vec<f32>,
    /// Samples decoded but not yet accepted by a full ring buffer.
    backlog: Vec<f32>,
    last_emit: Instant,
}

impl Engine {
    fn new(app: AppHandle, rx: Receiver<Cmd>, output_info: Arc<std::sync::Mutex<OutputInfo>>) -> Self {
        let shared = Arc::new(Shared {
            playing: AtomicBool::new(false),
            gain: AtomicU32::new(0.7f32.to_bits()),
            flush: AtomicBool::new(false),
            frames_played: AtomicU64::new(0),
            device_error: AtomicBool::new(false),
        });
        Self {
            app,
            rx,
            shared,
            output: None,
            output_info,
            wanted_device: None,
            source: None,
            scratch: Vec::with_capacity(16384),
            backlog: Vec::new(),
            last_emit: Instant::now(),
        }
    }

    fn run(mut self) {
        self.reopen_output();
        loop {
            let busy = self.shared.playing.load(Ordering::Relaxed) && self.source.is_some();
            let cmd = if busy {
                match self.rx.recv_timeout(Duration::from_millis(4)) {
                    Ok(c) => Some(c),
                    Err(RecvTimeoutError::Timeout) => None,
                    Err(RecvTimeoutError::Disconnected) => return,
                }
            } else {
                match self.rx.recv_timeout(Duration::from_millis(250)) {
                    Ok(c) => Some(c),
                    Err(RecvTimeoutError::Timeout) => None,
                    Err(RecvTimeoutError::Disconnected) => return,
                }
            };
            if let Some(cmd) = cmd {
                self.handle(cmd);
            }
            if self.shared.device_error.load(Ordering::SeqCst) {
                let pos = self.position();
                self.reopen_output();
                self.seek(pos);
            }
            self.fill();
            self.check_ended();
            if self.last_emit.elapsed() >= Duration::from_millis(250) {
                self.emit_state();
            }
        }
    }

    fn handle(&mut self, cmd: Cmd) {
        match cmd {
            Cmd::Load { path, autoplay } => {
                self.shared.playing.store(false, Ordering::SeqCst);
                let rate = self.output.as_ref().map(|o| o.rate).unwrap_or(48_000);
                match Source::open(&path, rate) {
                    Ok(src) => {
                        self.source = Some(src);
                        self.flush_ring();
                        self.update_output_info();
                        self.shared.playing.store(autoplay, Ordering::SeqCst);
                    }
                    Err(e) => {
                        self.source = None;
                        let _ = self.app.emit("player://error", format!("{e} ({path})"));
                    }
                }
                self.emit_state();
            }
            Cmd::Play => {
                if self.source.is_some() {
                    self.shared.playing.store(true, Ordering::SeqCst);
                }
                self.emit_state();
            }
            Cmd::Pause => {
                self.shared.playing.store(false, Ordering::SeqCst);
                self.emit_state();
            }
            Cmd::Seek(secs) => {
                self.seek(secs);
                self.emit_state();
            }
            Cmd::Volume(v) => {
                // Perceptual curve: slider 0..1 maps to gain v².
                let v = v.clamp(0.0, 1.0);
                self.shared.gain.store((v * v).to_bits(), Ordering::Relaxed);
            }
            Cmd::SetDevice(name) => {
                let pos = self.position();
                self.wanted_device = name;
                self.reopen_output();
                self.seek(pos);
                self.emit_state();
            }
            Cmd::Stop => {
                self.shared.playing.store(false, Ordering::SeqCst);
                self.source = None;
                self.flush_ring();
                self.emit_state();
            }
        }
    }

    fn reopen_output(&mut self) {
        self.output = None;
        match open_output(&self.shared, self.wanted_device.as_deref()) {
            Ok(out) => {
                if let Some(src) = self.source.as_mut() {
                    let _ = src.set_output_rate(out.rate);
                }
                self.output = Some(out);
            }
            Err(e) => {
                let _ = self.app.emit("player://error", format!("Audio output unavailable: {e}"));
            }
        }
        self.backlog.clear();
        self.update_output_info();
    }

    fn update_output_info(&self) {
        let info = match &self.output {
            Some(o) => {
                let source_rate = self.source.as_ref().map(|s| s.src_rate).unwrap_or(o.rate);
                OutputInfo {
                    device: o.name.clone(),
                    sample_rate: o.rate,
                    channels: o.channels,
                    source_rate,
                    resampling: source_rate != o.rate,
                    mode: if cfg!(windows) { "WASAPI Shared".into() } else { "Shared".into() },
                }
            }
            None => OutputInfo { device: "No output".into(), ..Default::default() },
        };
        if let Ok(mut g) = self.output_info.lock() {
            *g = info.clone();
        }
        let _ = self.app.emit("player://output", info);
    }

    /// Asks the callback to drop buffered audio and waits briefly until it has.
    fn flush_ring(&mut self) {
        self.backlog.clear();
        if self.output.is_none() {
            return;
        }
        self.shared.flush.store(true, Ordering::Release);
        let start = Instant::now();
        while self.shared.flush.load(Ordering::Acquire) && start.elapsed() < Duration::from_millis(120) {
            thread::sleep(Duration::from_millis(2));
        }
        // Callback idle (device stalled): clear from this side is not possible,
        // so reset the counter and let the callback drop data on its next run.
        self.shared.frames_played.store(0, Ordering::Release);
    }

    fn seek(&mut self, secs: f64) {
        if self.source.is_none() {
            return;
        }
        self.flush_ring();
        if let Some(src) = self.source.as_mut() {
            src.seek(secs);
        }
    }

    fn position(&self) -> f64 {
        let (Some(src), Some(out)) = (&self.source, &self.output) else { return 0.0 };
        let played = self.shared.frames_played.load(Ordering::Relaxed) as f64 / out.rate as f64;
        (src.base + played).min(if src.duration > 0.0 { src.duration } else { f64::MAX })
    }

    fn fill(&mut self) {
        let (Some(out), Some(src)) = (self.output.as_mut(), self.source.as_mut()) else { return };
        if self.shared.flush.load(Ordering::Acquire) {
            return;
        }
        // Push whatever was left over from last time first.
        if !self.backlog.is_empty() {
            let n = out.prod.push_slice(&self.backlog);
            self.backlog.drain(..n);
            if !self.backlog.is_empty() {
                return;
            }
        }
        // Keep the ring at least half full, decoding a few packets per pass.
        let mut rounds = 0;
        while !src.eof && out.prod.vacant_len() > out.prod.capacity().get() / 2 && rounds < 16 {
            self.scratch.clear();
            if let Err(e) = src.decode_into(&mut self.scratch) {
                let _ = self.app.emit("player://error", e);
                src.eof = true;
                break;
            }
            let n = out.prod.push_slice(&self.scratch);
            if n < self.scratch.len() {
                self.backlog.extend_from_slice(&self.scratch[n..]);
                break;
            }
            rounds += 1;
        }
    }

    fn check_ended(&mut self) {
        let ended = match (&self.source, &self.output) {
            (Some(src), Some(out)) => src.eof && self.backlog.is_empty() && out.prod.occupied_len() < 2,
            _ => false,
        };
        if ended && self.shared.playing.load(Ordering::Relaxed) {
            self.shared.playing.store(false, Ordering::SeqCst);
            let path = self.source.as_ref().map(|s| s.path.clone());
            self.emit_state();
            let _ = self.app.emit("player://ended", path);
        }
    }

    fn emit_state(&mut self) {
        self.last_emit = Instant::now();
        let state = PlayerState {
            path: self.source.as_ref().map(|s| s.path.clone()),
            playing: self.shared.playing.load(Ordering::Relaxed),
            position: self.position(),
            duration: self.source.as_ref().map(|s| s.duration).unwrap_or(0.0),
        };
        let _ = self.app.emit("player://state", state);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;

    /// Writes a 16-bit stereo sine WAV and returns its path.
    fn sine_wav(name: &str, rate: u32, secs: f64) -> String {
        let path = std::env::temp_dir().join(name);
        let frames = (rate as f64 * secs) as u32;
        let data_len = frames * 4;
        let mut f = File::create(&path).unwrap();
        f.write_all(b"RIFF").unwrap();
        f.write_all(&(36 + data_len).to_le_bytes()).unwrap();
        f.write_all(b"WAVEfmt ").unwrap();
        f.write_all(&16u32.to_le_bytes()).unwrap();
        f.write_all(&1u16.to_le_bytes()).unwrap();
        f.write_all(&2u16.to_le_bytes()).unwrap();
        f.write_all(&rate.to_le_bytes()).unwrap();
        f.write_all(&(rate * 4).to_le_bytes()).unwrap();
        f.write_all(&4u16.to_le_bytes()).unwrap();
        f.write_all(&16u16.to_le_bytes()).unwrap();
        f.write_all(b"data").unwrap();
        f.write_all(&data_len.to_le_bytes()).unwrap();
        for i in 0..frames {
            let s = ((i as f64 / rate as f64 * 440.0 * std::f64::consts::TAU).sin() * 12000.0) as i16;
            f.write_all(&s.to_le_bytes()).unwrap();
            f.write_all(&s.to_le_bytes()).unwrap();
        }
        path.to_string_lossy().into_owned()
    }

    fn decode_all(src: &mut Source) -> Vec<f32> {
        let mut out = Vec::new();
        while !src.eof {
            src.decode_into(&mut out).unwrap();
        }
        out
    }

    #[test]
    fn decodes_without_resampling_when_rates_match() {
        let path = sine_wav("cw_test_48k.wav", 48_000, 2.0);
        let mut src = Source::open(&path, 48_000).unwrap();
        assert!(src.resampler.is_none());
        assert!((src.duration - 2.0).abs() < 0.01, "duration {}", src.duration);
        let out = decode_all(&mut src);
        assert_eq!(out.len(), 48_000 * 2 * 2);
        let peak = out.iter().fold(0f32, |m, s| m.max(s.abs()));
        assert!((peak - 12000.0 / 32768.0).abs() < 0.01, "peak {peak}");
    }

    #[test]
    fn resamples_44k1_to_48k() {
        let path = sine_wav("cw_test_44k.wav", 44_100, 2.0);
        let mut src = Source::open(&path, 48_000).unwrap();
        assert!(src.resampler.is_some());
        let out = decode_all(&mut src);
        let frames = out.len() / 2;
        // FFT resampler adds some delay padding; allow a few chunks of slack.
        assert!((frames as i64 - 96_000).abs() < 4096, "frames {frames}");
        let peak = out.iter().fold(0f32, |m, s| m.max(s.abs()));
        assert!(peak > 0.3 && peak < 0.45, "peak {peak}");
    }

    #[test]
    fn seek_lands_near_target() {
        let path = sine_wav("cw_test_seek.wav", 48_000, 3.0);
        let mut src = Source::open(&path, 48_000).unwrap();
        let landed = src.seek(1.5);
        assert!((landed - 1.5).abs() < 0.05, "landed {landed}");
        let out = decode_all(&mut src);
        let frames = out.len() / 2;
        assert!((frames as i64 - 72_000).abs() < 2048, "frames {frames}");
    }
}
