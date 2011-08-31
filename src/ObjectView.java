package nz.gen.geek_central.ObjViewer;

import javax.microedition.khronos.opengles.GL10;
import nz.gen.geek_central.GLUseful.ObjReader;
import nz.gen.geek_central.GLUseful.Rotation;
import android.graphics.PointF;
import android.view.MotionEvent;

public class ObjectView extends android.opengl.GLSurfaceView
  {
    private ObjReader.Model TheObject = null;
    private boolean UseLighting = false;
    private boolean ClockwiseFaces = false;
    private Rotation CurRotation = new Rotation(0, 0, 0, 1);
    private PointF LastMouse = null;

    class ObjectViewRenderer implements Renderer
      {

        public ObjectViewRenderer()
          {
            super();
          /* nothing else to do, really */
          } /*ObjectViewRenderer*/

        public void onDrawFrame
          (
            GL10 gl
          )
          {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            if (TheObject != null)
              {
                if (UseLighting)
                  {
                    gl.glEnable(GL10.GL_LIGHTING);
                    gl.glEnable(GL10.GL_LIGHT0);
                  /* light position is fixed relative to view */
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT0,
                        /*pname =*/ GL10.GL_POSITION,
                        /*params =*/ new float[] {0.0f, 2.0f, 2.0f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT0,
                        /*pname =*/ GL10.GL_AMBIENT,
                        /*params =*/ new float[] {0.4f, 0.4f, 0.4f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT0,
                        /*pname =*/ GL10.GL_SPECULAR,
                        /*params =*/ new float[] {0.7f, 0.7f, 0.7f, 1.0f},
                        /*offset =*/ 0
                      );
                  }
                else
                  {
                    gl.glDisable(GL10.GL_LIGHTING);
                  } /*if*/
                CurRotation.Apply(gl);
                final float MaxDim =
                    (float)Math.max
                      (
                        Math.max
                          (
                            TheObject.BoundMax.x - TheObject.BoundMin.x,
                            TheObject.BoundMax.y - TheObject.BoundMin.y
                          ),
                        TheObject.BoundMax.z - TheObject.BoundMin.z
                      );
                System.err.printf
                  (
                    "ObjViewer.ObjectViewer: object bounds min (%.2f, %.2f, %.2f) max (%.2f, %.2f, %.2f), scale by %e\n",
                    TheObject.BoundMin.x, TheObject.BoundMin.y, TheObject.BoundMin.z,
                    TheObject.BoundMax.x, TheObject.BoundMax.y, TheObject.BoundMax.z,
                    1.0f / MaxDim
                  ); /* debug */
                gl.glScalef(1.0f / MaxDim, 1.0f / MaxDim, 1.0f / MaxDim);
                gl.glTranslatef
                  (
                    - (TheObject.BoundMax.x + TheObject.BoundMin.x) / 2.0f,
                    - (TheObject.BoundMax.y + TheObject.BoundMin.y) / 2.0f,
                    - TheObject.BoundMax.z * 1.2f
                  );
                gl.glFrontFace
                  (
                    ClockwiseFaces ? 
                        GL10.GL_CW
                    :
                        GL10.GL_CCW
                  );
                TheObject.Draw(gl);
              } /*if*/
          } /*onDrawFrame*/

        public void onSurfaceChanged
          (
            GL10 gl,
            int ViewWidth,
            int ViewHeight
          )
          {
            gl.glViewport(0, 0, ViewWidth, ViewHeight);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf
              (
                /*l =*/ - (float)ViewWidth / ViewHeight,
                /*r =*/ (float)ViewWidth / ViewHeight,
                /*b =*/ -1.0f,
                /*t =*/ 1.0f,
                /*n =*/ 1.0f,
                /*f =*/ 10.0f
              );
          } /*onSurfaceChanged*/

        public void onSurfaceCreated
          (
            GL10 gl,
            javax.microedition.khronos.egl.EGLConfig Config
          )
          {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            gl.glEnable(GL10.GL_CULL_FACE);
            gl.glShadeModel(GL10.GL_SMOOTH);
            gl.glEnable(GL10.GL_DEPTH_TEST);
          } /*onSurfaceCreated*/

      } /*ObjectViewRenderer*/

    public ObjectView
      (
        android.content.Context TheContext,
        android.util.AttributeSet TheAttributes
      )
      {
        super(TheContext, TheAttributes);
        setRenderer(new ObjectViewRenderer());
        setRenderMode(RENDERMODE_WHEN_DIRTY);
      } /*ObjectView*/

  /* save/restore instance state? */

    @Override
    public boolean onTouchEvent
      (
        MotionEvent TheEvent
      )
      {
        boolean Handled = false;
       /* fling gestures? */
        switch (TheEvent.getAction())
          {
        case MotionEvent.ACTION_DOWN:
            LastMouse = new PointF(TheEvent.getX(), TheEvent.getY());
            Handled = true;
        break;
        case MotionEvent.ACTION_MOVE:
            if (LastMouse != null && TheObject != null)
              {
                final PointF ThisMouse = new PointF(TheEvent.getX(), TheEvent.getY());
                final PointF MidPoint = new PointF(getWidth() / 2.0f, getHeight() / 2.0f);
                final float Radius =
                    (float)Math.hypot(ThisMouse.x - MidPoint.x, ThisMouse.y - MidPoint.y);
                final float DeltaR =
                        Radius
                    -
                        (float)Math.hypot(LastMouse.x - MidPoint.x, LastMouse.y - MidPoint.y);
                  /* radial movement, for rotation about X and Y axes */
                final float ZAngle =
                    (float)Math.toDegrees
                      (
                            Math.atan2
                              (
                                ThisMouse.y - MidPoint.y,
                                ThisMouse.x - MidPoint.x
                              )
                        -
                            Math.atan2
                              (
                                LastMouse.y - MidPoint.y,
                                LastMouse.x - MidPoint.x
                              )
                      );
                System.err.printf
                  (
                    "ObjectView: Radius = %e, DeltaR = %e, ZAngle = %e°, compose rotations: %s × %s × %s => %s\n",
                    Radius,
                    DeltaR,
                    ZAngle,
                    new Rotation /* X+Y axis */
                      (
                        (float)Math.toDegrees
                          (
                            Math.asin
                              (
                                    DeltaR
                                /
                                    (float)Math.hypot(MidPoint.x, MidPoint.y)
                                      /* scale rotation angle by assuming depth of
                                        axis is equal to radius of view */
                              )
                          ),
                        (MidPoint.y - ThisMouse.y) / Radius,
                        (MidPoint.x - ThisMouse.x) / Radius,
                        0
                      ),
                    new Rotation(ZAngle, 0, 0, -1), /* Z axis */
                    CurRotation,
                        new Rotation /* X+Y axis */
                          (
                            (float)Math.toDegrees
                              (
                                Math.asin
                                  (
                                        DeltaR
                                    /
                                        (float)Math.hypot(MidPoint.x, MidPoint.y)
                                          /* scale rotation angle by assuming depth of
                                            axis is equal to radius of view */
                                  )
                              ),
                            (MidPoint.y - ThisMouse.y) / Radius,
                            (MidPoint.x - ThisMouse.x) / Radius,
                            0
                          )
                    .mul
                      (
                        new Rotation(ZAngle, 0, 0, -1) /* Z axis */
                      )
                    .mul
                      (
                        CurRotation
                      )
                  ); /* debug */
                CurRotation =
                        new Rotation /* X+Y axis */
                          (
                            (float)Math.toDegrees
                              (
                                Math.asin
                                  (
                                        DeltaR
                                    /
                                        (float)Math.hypot(MidPoint.x, MidPoint.y)
                                          /* scale rotation angle by assuming depth of
                                            axis is equal to radius of view */
                                  )
                              ),
                            (MidPoint.y - ThisMouse.y) / Radius,
                            (MidPoint.x - ThisMouse.x) / Radius,
                            0
                          )
                    .mul
                      (
                        new Rotation(ZAngle, 0, 0, -1) /* Z axis */
                      )
                    .mul
                      (
                        CurRotation
                      );
                      /* ordering of composing the new rotations doesn't matter because axes are orthogonal */
                LastMouse = ThisMouse;
                requestRender();
              } /*if*/
            Handled = true;
        break;
        case MotionEvent.ACTION_UP:
            LastMouse = null;
            Handled = true;
        break;
          } /*switch*/
        return
            Handled;
      } /*onTouchEvent*/

    public void ResetOrientation()
      {
        CurRotation = new Rotation(0, 0, 0, 1);
        requestRender();
      } /*ResetOrientation*/

    public void SetObject
      (
        ObjReader.Model NewObject
      )
      {
        TheObject = NewObject;
        ResetOrientation();
      } /*SetObject*/

    public void SetUseLighting
      (
        boolean UseLighting
      )
      {
        this.UseLighting = UseLighting;
        requestRender();
      } /*SetUseLighting*/

    public boolean GetUseLighting()
      {
        return
            UseLighting;
      } /*GetUseLighting*/

    public void SetClockwiseFaces
      (
        boolean ClockwiseFaces
      )
      {
        this.ClockwiseFaces = ClockwiseFaces;
        requestRender();
      } /*SetClockwiseFaces*/

    public boolean GetClockwiseFaces()
      {
        return
            ClockwiseFaces;
      } /*GetClockwiseFaces*/

  } /*ObjectView*/
