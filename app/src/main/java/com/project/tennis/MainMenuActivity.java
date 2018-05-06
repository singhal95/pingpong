package com.project.tennis;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.project.tenis.R;

public class MainMenuActivity extends Activity implements SensorEventListener,
        RadioGroup.OnCheckedChangeListener, AdapterView.OnItemClickListener {
    public static final int ABOUT_DIALOG = 0;
    public static final int RATE_DIALOG = 1;

    private static final float PEOPLE_COEF = 1.3f;

    public static final int REQUEST_DISCOVEREBLY_BT = 2;

    private TennisView mTennisView;
    boolean m_actionTest;
    private View m_optionslayout;
    private View m_menu;
    private View m_statusbar;
    private View m_gameover;
    private Animation mAnimFadeIn = null;
    private Animation mAnimFadeOut = null;
    private boolean m_isGameStarted = false;
    private Button m_buttonResume;
    private TextView mLabel;
    private int touch1found = -1;
    private int touch2found = -1;
    private float touch1pos;
    private float touch2pos;
    private SensorManager mSensorManager;
    private boolean isOrientationActive = false;
    private int optionWantOrientation = 0;
    private View mBtView = null;
    private boolean mIsBtEnabledByMe = false;
    private final boolean mIsWiFiEnabledByMe = false;
    private boolean mIsBtEnabledBefore = false;
    private boolean mIsRemotePause = false;

    final ArrayList<String> mBtItems = new ArrayList<String>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        GameSettings.init(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);

        // ---------bluetooth & wifi----------
        ((ListView) findViewById(R.id.listLan)).setOnItemClickListener(this);
        //----------------------------

        mTennisView = (TennisView) findViewById(R.id.tennisview);
        mTennisView.setConfirmer(checker);

        m_menu = findViewById(R.id.mainMenu);
        m_statusbar = findViewById(R.id.statusBar);
        m_gameover = findViewById(R.id.gameEnd);
        m_optionslayout = findViewById(R.id.OptionsLayout);

        CustomFont.overrideFonts(getApplicationContext(),
                findViewById(R.id.mainLayout));
        mLabel = (TextView) findViewById(R.id.statusLabel);
        //mLabel.setTypeface(Typeface.createFromAsset(getAssets(), "S7.ttf"));

        mAnimFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mAnimFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        m_buttonResume = (Button) findViewById(R.id.mainMenuResume);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        ((RadioGroup) findViewById(R.id.RadioPlayers))
                .setOnCheckedChangeListener(this);
        ((RadioGroup) findViewById(R.id.RadioControlGroup))
                .setOnCheckedChangeListener(this);
        ((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                .setOnCheckedChangeListener(this);

        if (!GameSettings.get().isRunningCountRich()) {
            GameSettings.get().incrementRunningCount();
        }

        final SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sm.getSensorList(Sensor.TYPE_ACCELEROMETER).size() == 0) {
            findViewById(R.id.RadioControlGroup).setVisibility(View.GONE);
        }
        mBtView = findViewById(R.id.bluetoothBar);

    }

    public void buttonPauseClick(final View view) {
        showMainMenu();
    }

    @Override
    protected void onPause() {
        if (mBtView.getVisibility() != View.VISIBLE
                && findViewById(R.id.connectionLostLayout).getVisibility() != View.VISIBLE) {
            showMainMenu();
        }
        mTennisView.setRunning(false);
        super.onPause();
        disactivatedOrientationSensor();
        mTennisView.stopping();
    }

    @Override
    protected void onResume() {
        mTennisView.setRunning(true);
        super.onResume();
        if (m_isGameStarted) {
            activatedOrientationSensor();
        }
        mTennisView.running();
        if (GameSettings.get().isRunningCountRich()) {
            showDialog(RATE_DIALOG);
        } else {
            if (mBtView.getVisibility() != View.VISIBLE
                    && findViewById(R.id.connectionLostLayout).getVisibility() != View.VISIBLE) {
                showMainMenu();
            }
        }
    }

    @Override
    protected void onDestroy() {
        //mTennisView.stopGamerThread();

        super.onDestroy();
    }


    private void activatedOrientationSensor() {
        if (!isOrientationActive
                && (GameSettings.get().getInput() == GameSettings.INPUT.INPUT_TILT || GameSettings
                .get().getInput() == GameSettings.INPUT.INPUT_TILT2)
                && (GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_ONE
                || GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_BLUETOOTH
                || GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_WIFI || GameSettings
                .get().getPlayers() == GameSettings.PLAYERS.PLAYERS_ARCADE)) {
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_GAME);
            isOrientationActive = true;
            mLastSensorTimerStamp = SystemClock.uptimeMillis();
        }
    }

    private void disactivatedOrientationSensor() {
        if (isOrientationActive) {
            mSensorManager.unregisterListener(this);
            isOrientationActive = false;
        }
        mTennisView.setTildAngle(0.0f);
    }

    public void buttonAboutClick(final View view) {
        showDialog(ABOUT_DIALOG);
    }

    public void buttonExitClick(final View view) {
        mTennisView.setRunning(false);
        finish();
    }

    public void buttonResumeClick(final View view) {
        if (m_menu.getAnimation() != null) {
            return;
        }

        hideMainMenu(true);
    }

    public void buttonOnConnectionLost(final View view) {
        mAnimFadeOut.setAnimationListener(new setVisibilityGoneAfterAnimation(
                findViewById(R.id.connectionLostLayout)));
        findViewById(R.id.connectionLostLayout).startAnimation(mAnimFadeOut);

        m_isGameStarted = false;
        showMainMenu();
    }

    private boolean mBTDialogStarted = false;

    public void buttonNewGameClick(final View view) {
        findViewById(R.id.twDifficultyChanged).setVisibility(View.GONE);

        if (m_menu.getAnimation() != null) {
            return;
        }


        mIsRemotePause = false;
        if (GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_WIFI) {
            hideMainMenu(false);
            return;
        }
        m_isGameStarted = true;
        hideMainMenu(true);

        if (GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_ARCADE) {
            mTennisView.setNumberOfCpu(TennisView.ONE_PLAYER_ARCADE);
        }
        if (GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_ONE) {
            mTennisView.setNumberOfCpu(1);
        }
        if (GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_TWO) {
            mTennisView.setNumberOfCpu(0);
        }
        mTennisView.setRunning(true);
        m_statusbar.setVisibility(View.VISIBLE);
    }





    public void buttonOptionsClick(final View view) {
        showOptions();
    }

    protected void toggleMainMenu() {
        if (m_menu != null) {
            if (m_menu.getVisibility() == View.VISIBLE) {
                hideMainMenu(true);
            } else {
                showMainMenu();
            }
        }
    }

    protected void showMainMenu() {
        m_buttonResume
                .setVisibility(m_isGameStarted ? View.VISIBLE : View.GONE);

        hideGameOver();
        hideOptions();
        if (m_menu != null) {
            if (m_menu.getVisibility() != View.VISIBLE) {
                m_menu.setVisibility(View.VISIBLE);
                (findViewById(R.id.mainMenuSecondLayout))
                        .startAnimation(mAnimFadeIn);
            }
            (findViewById(R.id.versionText)).setVisibility(View.VISIBLE);
            m_statusbar.setVisibility(View.GONE);
        }
        mTennisView.setRunning(false);
        disactivatedOrientationSensor();
        findViewById(R.id.mainMenuResume).setEnabled(!mIsRemotePause);
        if (mIsRemotePause) {
            m_buttonResume.setText(R.string.menuPause);
        } else {
            m_buttonResume.setText(R.string.menuResume);
        }
    }

    protected void showOptions() {
        if (mBtView.getVisibility() == View.VISIBLE) {
            return;
        }
        hideMainMenu(false);

        if (m_optionslayout != null) {
            m_optionslayout.setVisibility(View.VISIBLE);
            m_statusbar.setVisibility(View.GONE);
            m_optionslayout.startAnimation(mAnimFadeIn);
        }
        ((CheckBox) findViewById(R.id.cSFX)).setChecked(GameSettings.get()
                .getSFX() == GameSettings.SFX.SFX_ON);
        if (GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_ARCADE) {
            ((RadioGroup) findViewById(R.id.RadioPlayers))
                    .check(R.id.btnOneArcade);
        }
        if (GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_ONE) {
            ((RadioGroup) findViewById(R.id.RadioPlayers))
                    .check(R.id.btnOnePlayer);
        }
        if (GameSettings.get().getPlayers() == GameSettings.PLAYERS.PLAYERS_TWO) {
            ((RadioGroup) findViewById(R.id.RadioPlayers))
                    .check(R.id.btnTwoPlayer);
        }



        if (GameSettings.get().getDifficulty() == GameSettings.DIFFICULTY.DIFFICULTY_EASY) {
            ((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                    .check(R.id.btnEasy);
        }
        if (GameSettings.get().getDifficulty() == GameSettings.DIFFICULTY.DIFFICULTY_NORMAL) {
            ((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                    .check(R.id.btnNormal);
        }
        if (GameSettings.get().getDifficulty() == GameSettings.DIFFICULTY.DIFFICULTY_HARD) {
            ((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                    .check(R.id.btnHard);
        }
        if (GameSettings.get().getDifficulty() == GameSettings.DIFFICULTY.DIFFICULTY_VERYHARD) {
            ((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                    .check(R.id.btnVeryHard);
        }
        if (GameSettings.get().getDifficulty() == GameSettings.DIFFICULTY.DIFFICULTY_NIGHTMARE) {
            ((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                    .check(R.id.btnNightmare);
        }
        if (GameSettings.get().getInput() == GameSettings.INPUT.INPUT_HAND) {
            ((RadioGroup) findViewById(R.id.RadioControlGroup))
                    .check(R.id.btnOnScreenr);
        }
        if (GameSettings.get().getInput() == GameSettings.INPUT.INPUT_TILT) {
            ((RadioGroup) findViewById(R.id.RadioControlGroup))
                    .check(R.id.btnTilt);
        }
        if (GameSettings.get().getInput() == GameSettings.INPUT.INPUT_TILT2) {
            ((RadioGroup) findViewById(R.id.RadioControlGroup))
                    .check(R.id.btnTilt2);
        }
        if (GameSettings.get().getPos() == GameSettings.POS.POS_LEFT) {
            ((RadioGroup) findViewById(R.id.RadioPosGroup)).check(R.id.btnLeft);
        }
        if (GameSettings.get().getPos() == GameSettings.POS.POS_RIGHT) {
            ((RadioGroup) findViewById(R.id.RadioPosGroup))
                    .check(R.id.btnRight);
        }
        if (GameSettings.get().getPos() == GameSettings.POS.POS_SWAP) {
            ((RadioGroup) findViewById(R.id.RadioPosGroup)).check(R.id.btnSwap);
        }
        ((SeekBar) findViewById(R.id.sbSensitivity))
                .setProgress(((SeekBar) findViewById(R.id.sbSensitivity))
                        .getMax()
                        - (int) (GameSettings.get().getSensivityTilt() - TennisView.TILT_MIN_SENSIVITY_ANGLE));
        onCheckedChanged(null, 0);
    }

    protected void hideMainMenu(final boolean resume) {
        if (m_menu.getVisibility() != View.GONE) {
            if (m_menu != null) {
                if (m_isGameStarted) {
                    m_statusbar.setVisibility(View.VISIBLE);
                }
                mAnimFadeOut
                        .setAnimationListener(new setVisibilityGoneAfterAnimation(
                                m_menu));
                (findViewById(R.id.versionText)).setVisibility(View.GONE);
                m_menu.startAnimation(mAnimFadeOut);
            }
        }
        mTennisView.setRunning(resume);
        if (resume) {
            activatedOrientationSensor();
            mTennisView.calcMargins();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    protected class setVisibilityGoneAfterAnimation implements
            AnimationListener {
        private final View mView;

        setVisibilityGoneAfterAnimation(final View view) {
            mView = view;
        }

        public void onAnimationEnd(final Animation animation) {
            mView.setVisibility(View.GONE);
            mView.clearAnimation();
        }

        public void onAnimationRepeat(final Animation animation) {
            // Auto-generated method stub
        }

        public void onAnimationStart(final Animation animation) {
            // Auto-generated method stub
        }
    }

    private void showGameOver() {
        if (m_gameover == null) {
            return;
        }
        m_gameover.setVisibility(View.VISIBLE);
        m_statusbar.setVisibility(View.GONE);
        mTennisView.setRunning(false);
        mTennisView.mxballpos = -1.0f;
        mTennisView.myballpos = -1.0f;
        m_isGameStarted = false;
        mTennisView.playSnd(mTennisView.SOUND_FINISH);
        disactivatedOrientationSensor();
    }

    private void hideGameOver() {
        if (m_gameover.getVisibility() == View.GONE) {
            return;
        }
        mAnimFadeOut.setAnimationListener(new setVisibilityGoneAfterAnimation(
                m_gameover));
        m_gameover.startAnimation(mAnimFadeOut);
    }

    private GameSettings.DIFFICULTY readDifficulty() {
        if (((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                .getCheckedRadioButtonId() == R.id.btnEasy) {
            return GameSettings.DIFFICULTY.DIFFICULTY_EASY;
        }
        if (((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                .getCheckedRadioButtonId() == R.id.btnNormal) {
            return GameSettings.DIFFICULTY.DIFFICULTY_NORMAL;
        }
        if (((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                .getCheckedRadioButtonId() == R.id.btnHard) {
            return GameSettings.DIFFICULTY.DIFFICULTY_HARD;
        }
        if (((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                .getCheckedRadioButtonId() == R.id.btnVeryHard) {
            return GameSettings.DIFFICULTY.DIFFICULTY_VERYHARD;
        }
        if (((RadioGroup) findViewById(R.id.RadioDifficultyGroup))
                .getCheckedRadioButtonId() == R.id.btnNightmare) {
            return GameSettings.DIFFICULTY.DIFFICULTY_NIGHTMARE;
        }
        return GameSettings.DIFFICULTY.DIFFICULTY_NORMAL;
    }

    private void hideOptions() {
        if (m_optionslayout.getVisibility() == View.GONE) {
            return;
        }
        mAnimFadeOut.setAnimationListener(new setVisibilityGoneAfterAnimation(
                m_optionslayout));
        m_optionslayout.startAnimation(mAnimFadeOut);

        GameSettings
                .get()
                .setSFX(((CheckBox) findViewById(R.id.cSFX)).isChecked() ? GameSettings.SFX.SFX_ON
                        : GameSettings.SFX.SFX_OFF);
        if (((RadioGroup) findViewById(R.id.RadioControlGroup))
                .getCheckedRadioButtonId() == R.id.btnOnScreenr) {
            GameSettings.get().setInput(GameSettings.INPUT.INPUT_HAND);
        }
        if (((RadioGroup) findViewById(R.id.RadioControlGroup))
                .getCheckedRadioButtonId() == R.id.btnTilt) {
            GameSettings.get().setInput(GameSettings.INPUT.INPUT_TILT);
        }
        if (((RadioGroup) findViewById(R.id.RadioControlGroup))
                .getCheckedRadioButtonId() == R.id.btnTilt2) {
            GameSettings.get().setInput(GameSettings.INPUT.INPUT_TILT2);
        }

        if (((RadioGroup) findViewById(R.id.RadioPlayers))
                .getCheckedRadioButtonId() == R.id.btnOneArcade) {
            GameSettings.get().setPlayers(GameSettings.PLAYERS.PLAYERS_ARCADE);
        }
        if (((RadioGroup) findViewById(R.id.RadioPlayers))
                .getCheckedRadioButtonId() == R.id.btnOnePlayer) {
            GameSettings.get().setPlayers(GameSettings.PLAYERS.PLAYERS_ONE);
        }
        if (((RadioGroup) findViewById(R.id.RadioPlayers))
                .getCheckedRadioButtonId() == R.id.btnTwoPlayer) {
            GameSettings.get().setPlayers(GameSettings.PLAYERS.PLAYERS_TWO);
        }



        if (((RadioGroup) findViewById(R.id.RadioPosGroup))
                .getCheckedRadioButtonId() == R.id.btnLeft) {
            GameSettings.get().setPos(GameSettings.POS.POS_LEFT);
        }
        if (((RadioGroup) findViewById(R.id.RadioPosGroup))
                .getCheckedRadioButtonId() == R.id.btnRight) {
            GameSettings.get().setPos(GameSettings.POS.POS_RIGHT);
        }
        if (((RadioGroup) findViewById(R.id.RadioPosGroup))
                .getCheckedRadioButtonId() == R.id.btnSwap) {
            GameSettings.get().setPos(GameSettings.POS.POS_SWAP);
        }

        GameSettings.get().setDifficulty(readDifficulty());
        GameSettings.get().setSensivityTilt(
                ((SeekBar) findViewById(R.id.sbSensitivity)).getMax()
                        - ((SeekBar) findViewById(R.id.sbSensitivity))
                        .getProgress()
                        + TennisView.TILT_MIN_SENSIVITY_ANGLE);

        GameSettings.get().commit();
    }

    @Override
    protected CustomDialog onCreateDialog(final int id) {
        CustomDialog dialog = null;
        switch (id) {
            case ABOUT_DIALOG: {
                final CustomDialog.Builder customBuilder = new CustomDialog.Builder(
                        this);
                customBuilder
                        .setTitle(getText(R.string.aboutTitle).toString())
                        .setMessage(getText(R.string.aboutText).toString())
                        .setImage(R.drawable.icon)
                        .setNeutralButton(getText(R.string.aboutClose).toString(),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int which) {
                                        dialog.dismiss();
                                    }
                                });
                dialog = customBuilder.create();
            }
            break;
            case RATE_DIALOG: {
                m_menu.setVisibility(View.GONE);
                final CustomDialog.Builder customBuilder = new CustomDialog.Builder(
                        this);
                customBuilder
                        .setTitle(getText(R.string.rateText).toString())
                        .setImage(R.drawable.icon)
                        .setPositiveButton(getText(R.string.rateOk).toString(),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int which) {
                                        dialog.dismiss();
                                        GameSettings.get().incrementRunningCount();
                                        showMainMenu();
                                    }
                                })
                        .setNeutralButton(getText(R.string.rateLater).toString(),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int which) {
                                        GameSettings.get().resetRunningCount();
                                        dialog.dismiss();
                                        showMainMenu();
                                    }
                                })
                        .setNegativeButton(getText(R.string.rateNever).toString(),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int which) {
                                        GameSettings.get().incrementRunningCount();
                                        dialog.dismiss();
                                        showMainMenu();
                                    }
                                });
                dialog = customBuilder.create();
            }
            break;
        }
        return dialog;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK)
                && (m_menu.getAnimation() != null || findViewById(
                R.id.mainMenuSecondLayout).getAnimation() != null)) {
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_MENU || (keyCode == KeyEvent.KEYCODE_BACK && m_menu
                .getVisibility() != View.VISIBLE))
                && m_isGameStarted
                && mBtView.getVisibility() != View.VISIBLE) {
            toggleMainMenu();
        } else if (keyCode == KeyEvent.KEYCODE_BACK
                && m_optionslayout.getVisibility() == View.VISIBLE) {
            showMainMenu();
        } else if (keyCode == KeyEvent.KEYCODE_BACK
                && m_menu.getVisibility() == View.VISIBLE && m_isGameStarted) {
            if (!mIsRemotePause) {
                buttonResumeClick(mTennisView);
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK
                && mBtView.getVisibility() == View.VISIBLE) {
            showMainMenu();
        } else if (keyCode == KeyEvent.KEYCODE_BACK
                && m_gameover.getVisibility() == View.VISIBLE) {
            buttonShowMainMenu(mTennisView);
        } else if (keyCode == KeyEvent.KEYCODE_BACK
                && findViewById(R.id.connectionLostLayout).getVisibility() == View.VISIBLE) {
            buttonOnConnectionLost(mTennisView);
        } else {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (mTennisView.getNumberOfCpu() == 2
                || (GameSettings.get().getInput() != GameSettings.INPUT.INPUT_HAND && mTennisView
                .getNumberOfCpu() != 0)) {
            return true;
        }
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
        int pointerId = event.getPointerId(pointerIndex);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (touch1found == -1 && mTennisView.getNumberOfCpu() != 0) {
                    touch1found = pointerId;
                    touch1pos = (int) event.getY(pointerIndex);
                } else if (touch2found == -1 && mTennisView.getNumberOfCpu() != 0) {
                    touch2found = pointerId;
                    touch2pos = (int) event.getY(pointerIndex);
                } else if ((int) event.getX(pointerIndex) <= mTennisView.getWidth() / 2
                        && mTennisView.getNumberOfCpu() == 0) {
                    touch1found = pointerId;
                    touch1pos = (int) event.getY(pointerIndex);
                } else if ((int) event.getX(pointerIndex) > mTennisView.getWidth() / 2
                        && mTennisView.getNumberOfCpu() == 0) {
                    touch2found = pointerId;
                    touch2pos = (int) event.getY(pointerIndex);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (touch1found == pointerId) {
                    touch1found = -1;
                }
                if (touch2found == pointerId) {
                    touch2found = -1;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final int pointerCount = event.getPointerCount();
                for (int i = 0; i < pointerCount; i++) {
                    pointerIndex = i;
                    pointerId = event.getPointerId(pointerIndex);
                    if (touch1found == pointerId) {
                        final float delta = event.getY(pointerIndex) - touch1pos;
                        mTennisView.leftMove(delta / mTennisView.getHeight()
                                * PEOPLE_COEF);
                        touch1pos = event.getY(pointerIndex);
                    }
                    if (touch2found == pointerId) {
                        final float delta = event.getY(pointerIndex) - touch2pos;
                        mTennisView.rightMove(delta / mTennisView.getHeight()
                                * PEOPLE_COEF);
                        touch2pos = event.getY(pointerIndex);
                    }
                }
                break;
        }

        return true;
    }

    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
    }

    private final float[] gravity = new float[3];
    private long mLastSensorTimerStamp = 0;
    private int mSensorPeriod = 0;

    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			/*
			 * final float gravity3D = (float) Math.sqrt(event.values[0]
			 * event.values[0] + event.values[1] * event.values[1] +
			 * event.values[2] * event.values[2]); if (gravity3D <
			 * SensorManager.GRAVITY_EARTH * 0.8f || gravity3D >
			 * SensorManager.GRAVITY_EARTH * 1.4f) { Log.i("tag", "fake0");
			 * return; }
			 */
            mSensorPeriod = (int) (0.8f * mSensorPeriod + 0.2f * (SystemClock
                    .uptimeMillis() - mLastSensorTimerStamp));

            float alpha = 0.70f + 0.2f * (60 - mSensorPeriod) / 50;
            if (alpha < 0.7f) {
                alpha = 0.7f;
            }
            if (alpha > 0.9f) {
                alpha = 0.9f;
            }

            //Log.i("tag", " sensor period " + mSensorPeriod + " alpha =" + alpha);

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
            if (optionWantOrientation > 0) {
                optionWantOrientation++;
            }

            final float gravity2D = (float) Math.sqrt(gravity[0] * gravity[0]
                    + gravity[2] * gravity[2]);
            final double acos = Math.acos(gravity[2] / gravity2D)
                    * Math.signum(gravity[0]);

            float val = (float) (-acos * 180.0f / Math.PI);
            // Log.i("tag", " " + val);

            if (optionWantOrientation > 0) {
                if (optionWantOrientation > 11) {
                    GameSettings.get().setAndCommitDefaultTilt(val);
                    if (!mTennisView.isRunning()) {
                        disactivatedOrientationSensor();
                    }
                    optionWantOrientation = 0;
                    Toast.makeText(this, R.string.ok, Toast.LENGTH_SHORT)
                            .show();
                }
                return;
            }
            val = GameSettings.get().getDefaultTilt() - val;

            if (val > 180.0f) {
                val = val - 360.0f;
            }
            if (val < -180.0f) {
                val = 360.0f + val;
            }
            if (mTennisView.isRunning()) {
                mTennisView.setTildAngle(val);
            }
            mLastSensorTimerStamp = SystemClock.uptimeMillis();
        }

    }

    final Runnable checker = new Runnable() {
        public void run() {
            if (mTennisView.getNumberOfCpu() == TennisView.ONE_PLAYER_ARCADE) {
                if (mTennisView.mArcadeLifes < 0) {
                    ((TextView) findViewById(R.id.gameWin)).setText(getText(
                            R.string.gameoverYouWin).toString());
                    showGameOver();
                }
                final CharSequence str = (mTennisView.mArcadeScore < 0 ? 0
                        : mTennisView.mArcadeScore)
                        + " \u25A0x"
                        + mTennisView.mArcadeLifes;
                mLabel.setText(str);
            } else {
                final CharSequence str = mTennisView.leftTabScore + ":"
                        + mTennisView.rightTabScore;
                mLabel.setText(str);
                if (mTennisView.playerTabPos() == TennisView.TAB_BOTH) {
                    if (mTennisView.leftTabScore >= TennisView.SCORE_TO_WIN) {
                        ((TextView) findViewById(R.id.gameWin)).setText(str
                                + " "
                                + getText(R.string.gameoverLeftWin).toString());
                        showGameOver();
                    }
                    if (mTennisView.rightTabScore >= TennisView.SCORE_TO_WIN) {
                        ((TextView) findViewById(R.id.gameWin))
                                .setText(str
                                        + " "
                                        + getText(R.string.gameoverRightWin)
                                        .toString());
                        showGameOver();
                    }
                } else if (mTennisView.playerTabPos() == TennisView.TAB_LEFT) {
                    if (mTennisView.leftTabScore >= TennisView.SCORE_TO_WIN) {
                        ((TextView) findViewById(R.id.gameWin)).setText(str
                                + " "
                                + getText(R.string.gameoverYouWin).toString());
                        showGameOver();
                    }
                    if (mTennisView.rightTabScore >= TennisView.SCORE_TO_WIN) {
                        ((TextView) findViewById(R.id.gameWin)).setText(str
                                + " "
                                + getText(R.string.gameoverYouLose).toString());
                        showGameOver();
                    }
                } else if (mTennisView.playerTabPos() == TennisView.TAB_RIGHT) {
                    if (mTennisView.leftTabScore >= TennisView.SCORE_TO_WIN) {
                        ((TextView) findViewById(R.id.gameWin)).setText(str
                                + " "
                                + getText(R.string.gameoverYouLose).toString());
                        showGameOver();
                    }
                    if (mTennisView.rightTabScore >= TennisView.SCORE_TO_WIN) {
                        ((TextView) findViewById(R.id.gameWin)).setText(str
                                + " "
                                + getText(R.string.gameoverYouWin).toString());
                        showGameOver();
                    }
                }
            }
        }
    };

    public void buttonOnPlayAgainClick(final View view) {
        hideGameOver();

        buttonNewGameClick(view);
        activatedOrientationSensor();

    }

    public void buttonShowMainMenu(final View view) {
        hideGameOver();
        showMainMenu();
    }

    public void onOptionsDefaultTilt(final View view) {
        optionWantOrientation = 1;
        activatedOrientationSensor();
    }

    public void onCheckedChanged(final RadioGroup arg0, final int arg1) {


        findViewById(R.id.options_tiltsetiings_lay)
                .setVisibility(
                        ((RadioButton) findViewById(R.id.btnOnScreenr))
                                .isChecked() ? View.GONE : View.VISIBLE);

        if (findViewById(R.id.RadioDifficultyGroup) == arg0
                && !mTennisView.isDemo()) {
            if (readDifficulty() != mTennisView.getDifficulty()) {
                if (findViewById(R.id.twDifficultyChanged).getVisibility() == View.GONE) {
                    findViewById(R.id.twDifficultyChanged).setVisibility(
                            View.VISIBLE);
                    findViewById(R.id.twDifficultyChanged).startAnimation(
                            mAnimFadeIn);
                }
            } else {
                if (findViewById(R.id.twDifficultyChanged).getVisibility() == View.VISIBLE) {
                    findViewById(R.id.twDifficultyChanged).setVisibility(
                            View.GONE);
                    findViewById(R.id.twDifficultyChanged).startAnimation(
                            mAnimFadeOut);
                }
            }
        }
    }







}