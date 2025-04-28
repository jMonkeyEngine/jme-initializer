package [GAME_PACKAGE];

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.ByValue;
import org.moe.natj.general.ann.Mapped;
import org.moe.natj.general.ann.Owned;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.Selector;
import org.moe.natj.objc.map.ObjCObjectMapper;

import apple.corefoundation.struct.CGPoint;
import apple.corefoundation.struct.CGRect;
import apple.corefoundation.struct.CGSize;
import apple.foundation.NSEnumerator;
import apple.foundation.NSNotification;
import apple.foundation.NSNotificationCenter;
import apple.foundation.NSSet;
import apple.glkit.GLKView;
import apple.glkit.GLKViewController;
import apple.glkit.enums.GLKViewDrawableDepthFormat;
import apple.glkit.protocol.GLKViewDelegate;
import apple.opengles.EAGLContext;
import apple.opengles.enums.EAGLRenderingAPI;
import apple.uikit.UIEvent;
import apple.uikit.UIScreen;
import apple.uikit.UITouch;
import apple.uikit.enums.UIRectEdge;
import apple.uikit.protocol.UIViewControllerTransitionCoordinator;

import com.jme3.app.LegacyApplication;
import com.jme3.system.JmeSystem;
import com.jme3.system.ios.JmeIosSystem;
import com.jme3.input.ios.IosInputHandler;
import com.jme3.math.Vector2f;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.system.JmeContext;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MoeIosJmeAppHarness extends GLKViewController implements GLKViewDelegate
{
    // Content from previous JmeAppHarness+IosHarness
    protected LegacyApplication app;
    private static final Logger logger = Logger.getLogger(MoeIosJmeAppHarness.class.getName());
    protected GLRenderer renderer;
    protected IosInputHandler input;
    protected boolean autoFlush = true;
    protected Vector2f resizePending = null;

    // Content from moe-samples (OpenGLBox)
    static final int GLKViewDrawableMultisample4X = 1;

    private EAGLContext context;

    private float rotation;

    private GLKView glkView;

    // Allocator, init and constructor (based on moe stuff)
    //@Selector("alloc")
    public static native MoeIosJmeAppHarness alloc();

    @Override
    @Owned
    @Selector("init")
    public MoeIosJmeAppHarness init()
    {
        MoeIosJmeAppHarness self = (MoeIosJmeAppHarness) super.init();
        if(self!=null)
        {
            final NSNotificationCenter notificationCenter = NSNotificationCenter.defaultCenter();
            notificationCenter.addObserverSelectorNameObject(self, new SEL("didRotate"), "UIDeviceOrientationDidChangeNotification", null);
        }
        return self;
    }

    public MoeIosJmeAppHarness(Pointer peer)
    {
        super(peer);
    }

    // Methods from previous JmeAppHarness+IosHarness
    public void appPaused() {
        logger.log(Level.FINE, "JmeAppHarness appPaused");
    }

    public void appReactivated() {
        logger.log(Level.FINE, "JmeAppHarness appReactivated");
    }

    public void appClosed() {
        logger.log(Level.FINE, "JmeAppHarness appClosed");
        app.stop();
    }

    public void appUpdate() {
        logger.log(Level.FINE, "JmeAppHarness appUpdate");
        //app.update();
    }

    public void appDraw() {
        logger.log(Level.FINE, "JmeAppHarness appDraw");
        if (renderer == null) {
            JmeContext iosContext = app.getContext();
            renderer = (GLRenderer)iosContext.getRenderer();
            renderer.initialize();
            input = (IosInputHandler)iosContext.getTouchInput();
            input.initialize();
        } else {
            if(resizePending != null) {
                appReshape((int)resizePending.x, (int)resizePending.y);
                resizePending = null;
            }
            app.update();
            if (autoFlush) {
                renderer.postFrame();
            }
        }
    }

    public void appReshape(int width, int height) {
        logger.log(Level.FINE, "JmeAppHarness reshape");
        AppSettings settings = app.getContext().getSettings();
        settings.setResolution(width, height);
        if (renderer != null) {
            app.reshape(width, height);
            resizePending = null;
        } else {
            resizePending = new Vector2f(width, height);
        }

        if (input != null) {
            input.loadSettings(settings);
        }
    }

    public void injectTouchBegin(int pointerId, long time, float x, float y) {
        if (input != null) {
            logger.log(Level.FINE, "JmeAppHarness injectTouchBegin");
            input.injectTouchDown(pointerId, time, x, y);
        }
    }

    public void injectTouchMove(int pointerId, long time, float x, float y) {
        if (input != null) {
            logger.log(Level.FINE, "JmeAppHarness injectTouchMove");
            input.injectTouchMove(pointerId, time, x, y);
        }
    }

    public void injectTouchEnd(int pointerId, long time, float x, float y) {
        if (input != null) {
            logger.log(Level.FINE, "JmeAppHarness injectTouchEnd");
            input.injectTouchUp(pointerId, time, x, y);
        }
    }

    // Methods from moe-samples (OpenGLBox)
    @Override
    public void viewDidLoad() {
        super.viewDidLoad();

        //setTitle("Jme3 for iOS using MOE");

        setEdgesForExtendedLayout(UIRectEdge.None);

        context = EAGLContext.alloc().initWithAPI(EAGLRenderingAPI.GLES3);
        if (context == null) {
            context = EAGLContext.alloc().initWithAPI(EAGLRenderingAPI.GLES2);
            if (context == null) {
                System.out.println("Failed to create ES context");
                System.exit(-1);
            }
        }
        glkView = (GLKView) view();
        glkView.setContext(context);
        glkView.setDrawableMultisample(GLKViewDrawableMultisample4X);
        glkView.setDrawableDepthFormat(GLKViewDrawableDepthFormat.Format24);

        setPreferredFramesPerSecond(60); // TODO: set desired FPS
        setPaused(false);

        setupGL();

        // Initialization from previous JmeAppHarness+IosHarness
        JmeSystem.setSystemDelegate(new JmeIosSystem());
        app = new [GAME_PACKAGE].game.[GAME_NAME]();
        AppSettings settings = new AppSettings(true);
        this.app.setSettings(settings);
        app.start();
        app.gainFocus();

        //Force a didRotate (jme3 reshape) event after having initialized everything so it renders properly from the beginning
        didRotate(null);
    }

    @Override
    public void viewWillTransitionToSizeWithTransitionCoordinator(@ByValue CGSize size, @Mapped(ObjCObjectMapper.class) UIViewControllerTransitionCoordinator coordinator) {
        if (isPaused())
            glkView.setNeedsDisplay();
    }

    @Override
    public void viewDidDisappear(boolean animated) {
        super.viewDidDisappear(animated);
        tearDownGL();
        if (EAGLContext.currentContext() != null && EAGLContext.currentContext().equals(context)) {
            EAGLContext.setCurrentContext(null);
        }
        context = null;
    }

    @Override
    public void didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning();
        if (isViewLoaded() && (view().window() == null)) {
            setView(null);
            tearDownGL();
            if (EAGLContext.currentContext().equals(context)) {
                EAGLContext.setCurrentContext(null);
            }
            context = null;
        }
        // Dispose of any resources that can be recreated.
    }

    private void setupGL() {
        EAGLContext.setCurrentContext(context);
        // Nothing to do here, there's no gl stuff created in this class
    }

    private void tearDownGL() {
        EAGLContext.setCurrentContext(context);
        // Nothing to do here, there's no gl stuff created in this class
    }

    @Override
    public void glkViewDrawInRect(GLKView view, CGRect rect) {
        appDraw();
    }

    @Selector("update")
    public void update() {
        appUpdate();
        appUpdate();
    }

    @Override
    public void touchesBeganWithEvent(NSSet touches, UIEvent event)
    {
        NSEnumerator enumerator = touches.objectEnumerator();
        UITouch touch = (UITouch) enumerator.nextObject();
        if(touch!=null)
        {
            do
            {
                CGPoint position = touch.locationInView(null);
                float scale = (float) glkView.contentScaleFactor();
                injectTouchBegin(touch.hashCode(), (long)touch.timestamp(), (float) (position.x() * scale), (float) (position.y() * scale));
                touch = (UITouch) enumerator.nextObject();
            } while (touch != null);
        }
    }

    @Override
    public void touchesCancelledWithEvent(NSSet touches, UIEvent event)
    {
        // Not implemented in jME3
    }

    @Override
    public void touchesEndedWithEvent(NSSet touches, UIEvent event)
    {
        NSEnumerator enumerator = touches.objectEnumerator();
        UITouch touch = (UITouch) enumerator.nextObject();
        if(touch!=null)
        {
            do
            {
                CGPoint position = touch.locationInView(null);
                float scale = (float) glkView.contentScaleFactor();
                injectTouchEnd(touch.hashCode(), (long)touch.timestamp(), (float) (position.x() * scale), (float) (position.y() * scale));
                touch = (UITouch) enumerator.nextObject();
            } while (touch != null);
        }
    }

    @Override
    public void touchesMovedWithEvent(NSSet touches, UIEvent event)
    {
        NSEnumerator enumerator = touches.objectEnumerator();
        UITouch touch = (UITouch) enumerator.nextObject();
        if(touch!=null)
        {
            do
            {
                CGPoint position = touch.locationInView(null);
                float scale = (float) glkView.contentScaleFactor();
                injectTouchMove(touch.hashCode(), (long)touch.timestamp(), (float) (position.x() * scale), (float) (position.y() * scale));
                touch = (UITouch) enumerator.nextObject();
            } while (touch != null);
        }
    }

    // java code equivalent to old jmeAppDelegate.m
    @Selector("didRotate")
    public void didRotate(NSNotification notification)
    {
        UIScreen screen = UIScreen.mainScreen();
        CGRect originalFrame = screen.bounds();
        if(glkView!=null)
        {
            CGRect frame = glkView.convertRectToView(originalFrame, null);
            float scale = (float) glkView.contentScaleFactor();
            appReshape((int) (frame.size().width() * scale), (int) (frame.size().height() * scale));
        }
    }
}
