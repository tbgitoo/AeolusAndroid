package com.mathis.midiSynth.userInterface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A custom View that draws a two-octave piano keyboard and handles touch input.
 * It supports multitouch, allowing multiple keys to be pressed simultaneously.
 * The view communicates key presses and releases to a listener.
 */
public class PianoView extends View {

    /**
     * Interface to notify a listener about piano key events.
     */
    public interface OnPianoKeyListener {
        /**
         * Called when a piano key is pressed down.
         * @param note The MIDI note number of the pressed key.
         */
        void onNoteOn(int note);

        /**
         * Called when a piano key is released.
         * @param note The MIDI note number of the released key.
         */
        void onNoteOff(int note);
    }

    /** The number of white keys to draw, spanning two octaves. */
    private static final int WHITE_KEYS_COUNT = 14; // 2 octaves

    /** The starting MIDI note for the keyboard (C4). */
    private static final int STARTING_MIDI_NOTE = 60; // C4

    /** Paint for drawing the white keys. */
    private Paint whiteKeyPaint;

    /** Paint for drawing the black keys. */
    private Paint blackKeyPaint;

    /** Paint for drawing the outlines of the keys. */
    private Paint strokePaint;

    /** A list of rectangles representing the white keys. */
    private final ArrayList<RectF> whiteKeys = new ArrayList<>();

    /** A list of rectangles representing the black keys. */
    private final ArrayList<RectF> blackKeys = new ArrayList<>();

    /** Maps the index of a white key to its MIDI note number. */
    private final int[] whiteKeyNoteMap = new int[WHITE_KEYS_COUNT];

    /** Maps the index of a black key to its MIDI note number. */
    private final int[] blackKeyNoteMap = new int[10];

    /** The listener to be notified of key events. */
    private OnPianoKeyListener listener;

    /**
     * Maps active pointer IDs to the MIDI note they are currently pressing.
     * This is essential for handling multitouch events correctly.
     */
    private final Map<Integer, Integer> activePointers = new HashMap<>();

    /**
     * Constructor for creating a PianoView programmatically.
     * @param context The Context the view is running in.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public PianoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Sets the listener that will receive key press and release events.
     * @param listener The listener to set.
     */
    public void setOnPianoKeyListener(OnPianoKeyListener listener) {
        this.listener = listener;
    }

    /**
     * Initializes the Paint objects for drawing and builds the key-to-note mapping.
     */
    private void init() {
        whiteKeyPaint = new Paint();
        whiteKeyPaint.setColor(Color.WHITE);
        whiteKeyPaint.setStyle(Paint.Style.FILL);

        blackKeyPaint = new Paint();
        blackKeyPaint.setColor(Color.BLACK);
        blackKeyPaint.setStyle(Paint.Style.FILL);

        strokePaint = new Paint();
        strokePaint.setColor(Color.DKGRAY);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);

        buildKeymap();
    }

    /**
     * Creates the mapping between key indices and their corresponding MIDI note numbers
     * for two full octaves, starting from C4.
     */
    private void buildKeymap() {
        int currentNote = STARTING_MIDI_NOTE;
        int whiteKeyIndex = 0;
        int blackKeyIndex = 0;

        for (int i = 0; i < 2; i++) { // For 2 octaves
            // The pattern of a piano octave is W-B-W-B-W-W-B-W-B-W-B-W
            whiteKeyNoteMap[whiteKeyIndex++] = currentNote;      // C
            blackKeyNoteMap[blackKeyIndex++] = currentNote + 1;  // C#
            whiteKeyNoteMap[whiteKeyIndex++] = currentNote + 2;  // D
            blackKeyNoteMap[blackKeyIndex++] = currentNote + 3;  // D#
            whiteKeyNoteMap[whiteKeyIndex++] = currentNote + 4;  // E
            whiteKeyNoteMap[whiteKeyIndex++] = currentNote + 5;  // F
            blackKeyNoteMap[blackKeyIndex++] = currentNote + 6;  // F#
            whiteKeyNoteMap[whiteKeyIndex++] = currentNote + 7;  // G
            blackKeyNoteMap[blackKeyIndex++] = currentNote + 8;  // G#
            whiteKeyNoteMap[whiteKeyIndex++] = currentNote + 9;  // A
            blackKeyNoteMap[blackKeyIndex++] = currentNote + 10; // A#
            whiteKeyNoteMap[whiteKeyIndex++] = currentNote + 11; // B
            currentNote += 12;
        }
    }

    /**
     * Called when the size of the view changes. This is used to recalculate the
     * dimensions and positions of all the piano keys.
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        whiteKeys.clear();
        blackKeys.clear();

        float whiteKeyWidth = (float) w / WHITE_KEYS_COUNT;
        float blackKeyWidth = whiteKeyWidth * 0.6f;
        float blackKeyHeight = h * 0.6f;

        // Create white key rectangles
        for (int i = 0; i < WHITE_KEYS_COUNT; i++) {
            float left = i * whiteKeyWidth;
            whiteKeys.add(new RectF(left, 0, left + whiteKeyWidth, h));
        }

        // Create black key rectangles based on the standard piano pattern
        for (int i = 0; i < 2; i++) { // Two octaves
            float octaveOffset = i * 7 * whiteKeyWidth;
            // C#
            blackKeys.add(new RectF(octaveOffset + whiteKeyWidth - (blackKeyWidth / 2), 0, octaveOffset + whiteKeyWidth + (blackKeyWidth / 2), blackKeyHeight));
            // D#
            blackKeys.add(new RectF(octaveOffset + 2 * whiteKeyWidth - (blackKeyWidth / 2), 0, octaveOffset + 2 * whiteKeyWidth + (blackKeyWidth / 2), blackKeyHeight));
            // F#
            blackKeys.add(new RectF(octaveOffset + 4 * whiteKeyWidth - (blackKeyWidth / 2), 0, octaveOffset + 4 * whiteKeyWidth + (blackKeyWidth / 2), blackKeyHeight));
            // G#
            blackKeys.add(new RectF(octaveOffset + 5 * whiteKeyWidth - (blackKeyWidth / 2), 0, octaveOffset + 5 * whiteKeyWidth + (blackKeyWidth / 2), blackKeyHeight));
            // A#
            blackKeys.add(new RectF(octaveOffset + 6 * whiteKeyWidth - (blackKeyWidth / 2), 0, octaveOffset + 6 * whiteKeyWidth + (blackKeyWidth / 2), blackKeyHeight));
        }
    }

    /**
     * Renders the piano keyboard on the canvas.
     * @param canvas The canvas on which the background will be drawn.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // Draw white keys first, so they appear underneath
        for (RectF rect : whiteKeys) {
            canvas.drawRect(rect, whiteKeyPaint);
            canvas.drawRect(rect, strokePaint);
        }

        // Draw black keys on top
        for (RectF rect : blackKeys) {
            canvas.drawRect(rect, blackKeyPaint);
        }
    }

    /**
     * Handles touch events to detect key presses and releases.
     * Supports multitouch for pressing multiple keys at once.
     * @param event The MotionEvent object containing full information about the event.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handleTouch(event.getX(pointerIndex), event.getY(pointerIndex), pointerId, true);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                handleTouch(event.getX(pointerIndex), event.getY(pointerIndex), pointerId, false);
                break;
        }
        return true;
    }

    /**
     * Determines which key was touched and notifies the listener.
     * @param x The x-coordinate of the touch event.
     * @param y The y-coordinate of the touch event.
     * @param pointerId The unique ID of the pointer for this touch event.
     * @param isDown True for a "down" event, false for an "up" event.
     */
    private void handleTouch(float x, float y, int pointerId, boolean isDown) {
        int note = -1;

        if (isDown) {
            // Check black keys first, as they are drawn on top and have a smaller area
            for (int i = 0; i < blackKeys.size(); i++) {
                if (blackKeys.get(i).contains(x, y)) {
                    note = blackKeyNoteMap[i];
                    break;
                }
            }
            // If no black key was hit, check the white keys
            if (note == -1) {
                for (int i = 0; i < whiteKeys.size(); i++) {
                    if (whiteKeys.get(i).contains(x, y)) {
                        note = whiteKeyNoteMap[i];
                        break;
                    }
                }
            }

            // If a key was pressed, notify the listener and track the pointer
            if (listener != null && note != -1) {
                listener.onNoteOn(note);
                activePointers.put(pointerId, note);
            }
        } else { // Touch up event
            // Find which note this pointer was pressing and notify the listener
            Integer releasedNote = activePointers.get(pointerId);
            if (listener != null && releasedNote != null) {
                listener.onNoteOff(releasedNote);
                activePointers.remove(pointerId);
            }
        }
    }
}
