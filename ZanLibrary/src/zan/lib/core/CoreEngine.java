package zan.lib.core;

import java.nio.ByteBuffer;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import zan.lib.input.InputManager;
import zan.lib.panel.BasePanel;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

/** A framework for simple Java game development.
 * 
 * @author Rozan I. Rosandi
 *
 */
public abstract class CoreEngine {
	
	// CALLBACK CLASSES
	
	private GLFWErrorCallback errorCallback;
	
	private GLFWKeyCallback keyCallback;
	private GLFWCharCallback charCallback;
	private GLFWMouseButtonCallback mouseButtonCallback;
	private GLFWCursorPosCallback mousePosCallback;
	private GLFWScrollCallback mouseScrollCallback;
	private GLFWCursorEnterCallback mouseEnterCallback;
	
	private GLFWWindowCloseCallback windowCloseCallback;
	private GLFWWindowRefreshCallback windowRefreshCallback;
	private GLFWWindowSizeCallback windowSizeCallback;
	private GLFWFramebufferSizeCallback framebufferSizeCallback;
	private GLFWWindowPosCallback windowPosCallback;
	private GLFWWindowIconifyCallback windowIconifyCallback;
	private GLFWWindowFocusCallback windowFocusCallback;
	
	// WINDOW VARIABLES
	
	private String SCR_TITLE = "";
	
	private int SCR_X = -1;
	private int SCR_Y = -1;
	
	private int WIN_WIDTH = 640;
	private int WIN_HEIGHT = 480;
	private int MNT_WIDTH;
	private int MNT_HEIGHT;
	private int SCR_WIDTH = WIN_WIDTH;
	private int SCR_HEIGHT = WIN_HEIGHT;
	
	// WINDOW FLAGS
	
	private boolean SCR_VISIBLE = true;
	private boolean SCR_MINIMIZE = false;
	private boolean SCR_FULL = false;
	private boolean SCR_CROP = false;
	
	private boolean SCR_RESIZABLE = false;
	private boolean SCR_DECORATED = true;
	private boolean SCR_FLOATING = false;
	private boolean SCR_AUTOMINIMIZE = true;
	
	private int SWAP_INTERVAL = 0;
	
	private boolean REQ_WINDOW_RESET = false;
	
	// TIMING VARIABLES
	
	private long ticks = 0L;
	private double fps = 0.0;
	private double TICKS_PER_SECOND = 50.0;
	private double DELTA_TIME = (1.0/TICKS_PER_SECOND);
	private int MAX_FRAME_SKIP = 5;
	
	// WINDOW & PANEL
	
	private long window;
	
	private BasePanel panel;
	
	private boolean initialized = false;
	
	// RUN METHOD
	
	protected void run() {
		if (initialized) return;
		try {
			init();
			loop();
			destroy();
		} finally {
			glfwTerminate();
			errorCallback.release();
		}
	}
	
	// CLEANUP
	
	private void destroy() {
		glfwDestroyWindow(window);
		keyCallback.release();
		charCallback.release();
		mouseButtonCallback.release();
		mousePosCallback.release();
		mouseScrollCallback.release();
		mouseEnterCallback.release();
		windowCloseCallback.release();
		windowRefreshCallback.release();
		windowSizeCallback.release();
		framebufferSizeCallback.release();
		windowPosCallback.release();
		windowIconifyCallback.release();
		windowFocusCallback.release();
	}
	
	public void close() {glfwSetWindowShouldClose(window, GL_TRUE);}
	public void close(boolean close) {
		if (close) close();
		else glfwSetWindowShouldClose(window, GL_FALSE);
	}
	
	// INITIALIZATION
	
	private void init() {
		glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));
		
		if (glfwInit() != GL_TRUE) throw new IllegalStateException("Unable to initialize GLFW");
		
		ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		MNT_WIDTH = GLFWvidmode.width(vidmode);
		MNT_HEIGHT = GLFWvidmode.height(vidmode);
		if (SCR_X == -1) SCR_X = (MNT_WIDTH-WIN_WIDTH)/2;
		if (SCR_Y == -1) SCR_Y = (MNT_HEIGHT-WIN_HEIGHT)/2;
		
		createWindow();
		initInput();
		initWindow();
		
		GLContext.createFromCurrent();
		initGL();
		
		REQ_WINDOW_RESET = false;
		initialized = true;
	}
	
	private void createWindow() {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		glfwWindowHint(GLFW_FOCUSED, GL_TRUE);
		glfwWindowHint(GLFW_RESIZABLE, SCR_RESIZABLE ? GL_TRUE : GL_FALSE);
		glfwWindowHint(GLFW_DECORATED, SCR_DECORATED ? GL_TRUE : GL_FALSE);
		glfwWindowHint(GLFW_FLOATING, SCR_FLOATING ? GL_TRUE : GL_FALSE);
		glfwWindowHint(GLFW_AUTO_ICONIFY, SCR_AUTOMINIMIZE ? GL_TRUE : GL_FALSE);
		
		if (SCR_FULL) {
			if (SCR_CROP) {
				SCR_WIDTH = WIN_WIDTH;
				SCR_HEIGHT = WIN_HEIGHT;
			} else {
				SCR_WIDTH = MNT_WIDTH;
				SCR_HEIGHT = MNT_HEIGHT;
			}
			window = glfwCreateWindow(SCR_WIDTH, SCR_HEIGHT, SCR_TITLE, glfwGetPrimaryMonitor(), NULL);
		} else {
			SCR_WIDTH = WIN_WIDTH;
			SCR_HEIGHT = WIN_HEIGHT;
			window = glfwCreateWindow(SCR_WIDTH, SCR_HEIGHT, SCR_TITLE, NULL, NULL);
		}
		
		if (window == NULL) throw new RuntimeException("Failed to create the GLFW window");
	}
	
	private void initInput() {
		InputManager.init();
		InputManager.setWindow(window);
		
		glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int state, int mods) {
				if (initialized) {
					InputManager.invokeKeys(window, key, state, mods);
					onKey(window, key, state, mods, scancode);
				}
			}
		});
		glfwSetCharCallback(window, charCallback = new GLFWCharCallback() {
			@Override
			public void invoke(long window, int ch) {
				if (initialized) {
					InputManager.invokeChars(window, (char)ch);
					onChar(window, (char)ch);
				}
			}
		});
		glfwSetMouseButtonCallback(window, mouseButtonCallback = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int state, int mods) {
				if (initialized) {
					InputManager.invokeMouseButtons(window, button, state, mods);
					onMouseButton(window, button, state, mods);
				}
			}
		});
		glfwSetCursorPosCallback(window, mousePosCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double mouseX, double mouseY) {
				if (initialized) {
					InputManager.invokeMousePos(window, mouseX, mouseY);
					onMouseMove(window, mouseX, mouseY);
				}
			}
		});
		glfwSetScrollCallback(window, mouseScrollCallback = new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double scrollX, double scrollY) {
				if (initialized) {
					InputManager.invokeMouseScroll(window, scrollY);
					onMouseScroll(window, scrollX, scrollY);
				}
			}
		});
		glfwSetCursorEnterCallback(window, mouseEnterCallback = new GLFWCursorEnterCallback() {
			@Override
			public void invoke(long window, int mouseEnter) {
				if (initialized) {
					InputManager.invokeMouseEnter(window, (mouseEnter == GL_TRUE));
					onMouseEnter(window, (mouseEnter == GL_TRUE));
				}
			}
		});
		
		glfwSetWindowCloseCallback(window, windowCloseCallback = new GLFWWindowCloseCallback() {
			@Override
			public void invoke(long window) {
				if (initialized) onWindowClose(window);
			}
		});
		glfwSetWindowRefreshCallback(window, windowRefreshCallback = new GLFWWindowRefreshCallback() {
			@Override
			public void invoke(long window) {
				if (initialized) onWindowRefresh(window);
			}
		});
		glfwSetWindowSizeCallback(window, windowSizeCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (initialized && !SCR_FULL) {
					WIN_WIDTH = width;
					WIN_HEIGHT = height;
					onWindowResize(window, width, height);
				}
			}
		});
		glfwSetFramebufferSizeCallback(window, framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (initialized && !SCR_FULL) {
					SCR_WIDTH = width;
					SCR_HEIGHT = height;
					onScreenResize(window, width, height);
				}
			}
		});
		glfwSetWindowPosCallback(window, windowPosCallback = new GLFWWindowPosCallback() {
			@Override
			public void invoke(long window, int posX, int posY) {
				if (initialized && !SCR_FULL) {
					SCR_X = posX;
					SCR_Y = posY;
					onWindowMove(window, posX, posY);
				}
			}
		});
		glfwSetWindowIconifyCallback(window, windowIconifyCallback = new GLFWWindowIconifyCallback() {
			@Override
			public void invoke(long window, int minimize) {
				if (initialized) {
					SCR_MINIMIZE = (minimize == GL_TRUE);
					onWindowMinimize(window, (minimize == GL_TRUE));
				}
			}
		});
		glfwSetWindowFocusCallback(window, windowFocusCallback = new GLFWWindowFocusCallback() {
			@Override
			public void invoke(long window, int focus) {
				if (initialized) onWindowFocus(window, (focus == GL_TRUE));
			}
		});
	}
	
	private void initWindow() {
		if (!SCR_FULL) glfwSetWindowPos(window, SCR_X, SCR_Y);
		glfwMakeContextCurrent(window);
		glfwSwapInterval(SWAP_INTERVAL);
		if (SCR_MINIMIZE) glfwIconifyWindow(window);
		if (SCR_VISIBLE) glfwShowWindow(window);
	}
	
	private void initGL() {
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glClearColor(0f, 0f, 0f, 0f);
	}
	
	private void resetWindow() {
		glfwDestroyWindow(window);
		createWindow();
		initInput();
		initWindow();
		initGL();
		
		onWindowResize(window, WIN_WIDTH, WIN_HEIGHT);
		onScreenResize(window, SCR_WIDTH, SCR_HEIGHT);
		onWindowMove(window, SCR_X, SCR_Y);
		onWindowMinimize(window, isMinimized());
		onWindowFocus(window, isFocused());
		
		REQ_WINDOW_RESET = false;
	}
	
	// MAIN LOOP
	
	private void loop() {
		double nextTick, nextFrame;
		nextTick = nextFrame = getTime();
		int frameCount = 0;
		
		while (glfwWindowShouldClose(window) == GL_FALSE) {
			double time = getTime();
			
			int frameSkip = 0;
			while (time > nextTick && frameSkip < MAX_FRAME_SKIP) {
				InputManager.clear(window);
				glfwPollEvents();
				
				update(time);
				
				ticks++;
				frameSkip++;
				nextTick += DELTA_TIME;
			}
			
			// FPS counter
			frameCount++;
			if (time > nextFrame) {
				fps = frameCount;
				frameCount = 0;
				nextFrame += 1.0;
			}
			
			render((time+DELTA_TIME-nextTick)/(DELTA_TIME));
			check();
		}
	}
	
	private void update(double time) {
		panel.update(time);
	}
	
	private void render(double ip) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        panel.render(ip);
        
        glfwSwapBuffers(window);
	}
	
	private void check() {
		BasePanel nextPanel = panel.changePanel();
		if (nextPanel != null) setPanel(nextPanel);
		
		if (REQ_WINDOW_RESET) resetWindow();
	}
	
	// SETTERS
	
	public void setPanel(BasePanel panel) {
		if (this.panel != null) this.panel.destroy();
		this.panel = panel;
		panel.init();
	}
	
	public void setTitle(String title) {
		SCR_TITLE = title;
		if (initialized) glfwSetWindowTitle(window, SCR_TITLE);
	}
	
	public void setWindowPos(int posX, int posY) {
		SCR_X = posX;
		SCR_Y = posY;
		if (initialized) glfwSetWindowPos(window, SCR_X, SCR_Y);
	}
	
	public void setScreenSize(int width, int height) {
		WIN_WIDTH = width;
		WIN_HEIGHT = height;
		SCR_WIDTH = WIN_WIDTH;
		SCR_HEIGHT = WIN_HEIGHT;
		if (initialized) glfwSetWindowSize(window, SCR_WIDTH, SCR_HEIGHT);
	}
	
	// TOGGLERS
	
	public void setFullScreen(boolean fullscreen) {SCR_FULL = fullscreen; REQ_WINDOW_RESET = true;}
	public void toggleFullScreen() {setFullScreen(!SCR_FULL);}
	
	public void setScreenCrop(boolean screencrop) {SCR_CROP = screencrop; REQ_WINDOW_RESET = true;}
	public void toggleScreenCrop() {setScreenCrop(!SCR_CROP);}
	
	public void setVSync(boolean vsync) {
		SWAP_INTERVAL = vsync ? 1 : 0;
		if (initialized) glfwSwapInterval(SWAP_INTERVAL);
	}
	public void toggleVSync() {setVSync(!isVSync());}
	
	public void setVisible(boolean visible) {
		SCR_VISIBLE = visible;
		if (initialized) {
			if (SCR_VISIBLE) glfwShowWindow(window);
			else glfwHideWindow(window);
		}
	}
	public void toggleVisible() {setVisible(!isVisible());}
	
	public void setMinimize(boolean minimize) {
		SCR_MINIMIZE = minimize;
		if (initialized) {
			if (SCR_MINIMIZE) glfwIconifyWindow(window);
			else glfwRestoreWindow(window);
		}
	}
	public void toggleMinimize() {setMinimize(!isMinimized());}
	
	/** @deprecated No implementation of this method yet. */
	public void setFocus(boolean focused) {}
	/** @deprecated No implementation of this method yet. */
	public void toggleFocus() {}
	
	public void setResizable(boolean resizable) {SCR_RESIZABLE = resizable; REQ_WINDOW_RESET = true;}
	public void toggleResizable() {setResizable(!SCR_RESIZABLE);}
	
	public void setDecorated(boolean decorated) {SCR_DECORATED = decorated; REQ_WINDOW_RESET = true;}
	public void toggleDecorated() {setDecorated(!SCR_DECORATED);}
	
	public void setFloating(boolean floating) {SCR_FLOATING = floating; REQ_WINDOW_RESET = true;}
	public void toggleFloating() {setFloating(!SCR_FLOATING);}
	
	public void setAutoMinimize(boolean autominimize) {SCR_AUTOMINIMIZE = autominimize; REQ_WINDOW_RESET = true;}
	public void toggleAutoMinimize() {setAutoMinimize(!SCR_AUTOMINIMIZE);}
	
	// TIMING METHODS
	
	public void setTPS(double tps) {
		TICKS_PER_SECOND = tps;
		DELTA_TIME = (1.0/TICKS_PER_SECOND);
	}
	
	public void setMaxFrameSkip(int mfs) {MAX_FRAME_SKIP = mfs;}
	
	// CALLBACK METHODS
	
	protected void onKey(int key, int state, int mods, int scancode) {if (panel != null) panel.onKey(key, state, mods, scancode);}
	protected void onChar(char ch) {if (panel != null) panel.onChar(ch);}
	protected void onMouseButton(int button, int state, int mods) {if (panel != null) panel.onMouseButton(button, state, mods);}
	protected void onMouseMove(double mouseX, double mouseY) {if (panel != null) panel.onMouseMove(mouseX, mouseY);}
	protected void onMouseScroll(double scrollX, double scrollY) {if (panel != null) panel.onMouseScroll(scrollX, scrollY);}
	protected void onMouseEnter(boolean mouseEnter) {if (panel != null) panel.onMouseEnter(mouseEnter);}
	
	protected void onWindowClose() {if (panel != null) panel.onWindowClose();}
	protected void onWindowRefresh() {if (panel != null) panel.onWindowRefresh();}
	protected void onWindowResize(int width, int height) {if (panel != null) panel.onWindowResize(width, height);}
	protected void onScreenResize(int width, int height) {if (panel != null) panel.onScreenResize(width, height);}
	protected void onWindowMove(int posX, int posY) {if (panel != null) panel.onWindowMove(posX, posY);}
	protected void onWindowMinimize(boolean minimize) {if (panel != null) panel.onWindowMinimize(minimize);}
	protected void onWindowFocus(boolean focus) {if (panel != null) panel.onWindowFocus(focus);}
	
	protected void onKey(long window, int key, int state, int mods, int scancode) {if (this.window == window) onKey(key, state, mods, scancode);}
	protected void onChar(long window, char ch) {if (this.window == window) onChar(ch);}
	protected void onMouseButton(long window, int button, int state, int mods) {if (this.window == window) onMouseButton(button, state, mods);}
	protected void onMouseMove(long window, double mouseX, double mouseY) {if (this.window == window) onMouseMove(mouseX, mouseY);}
	protected void onMouseScroll(long window, double scrollX, double scrollY) {if (this.window == window) onMouseScroll(scrollX, scrollY);}
	protected void onMouseEnter(long window, boolean mouseEnter) {if (this.window == window) onMouseEnter(mouseEnter);}
	
	protected void onWindowClose(long window) {if (this.window == window) onWindowClose();}
	protected void onWindowRefresh(long window) {if (this.window == window) onWindowRefresh();}
	protected void onWindowResize(long window, int width, int height) {if (this.window == window) onWindowResize(width, height);}
	protected void onScreenResize(long window, int width, int height) {if (this.window == window) onScreenResize(width, height);}
	protected void onWindowMove(long window, int posX, int posY) {if (this.window == window) onWindowMove(posX, posY);}
	protected void onWindowMinimize(long window, boolean minimize) {if (this.window == window) onWindowMinimize(minimize);}
	protected void onWindowFocus(long window, boolean focus) {if (this.window == window) onWindowFocus(focus);}
	
	// GETTERS
	
	public long getWindow() {return window;}
	
	public boolean isInitialized() {return initialized;}
	
	public BasePanel getPanel() {return panel;}
	
	public String getTitle() {return SCR_TITLE;}
	
	public int getWindowX() {return SCR_X;}
	public int getWindowY() {return SCR_Y;}
	
	public int getWindowWidth() {return WIN_WIDTH;}
	public int getWindowHeight() {return WIN_HEIGHT;}
	public double getWindowRatio() {return (double)WIN_WIDTH/(double)WIN_HEIGHT;}
	
	public int getMonitorWidth() {return MNT_WIDTH;}
	public int getMonitorHeight() {return MNT_HEIGHT;}
	public double getMonitorRatio() {return (double)MNT_WIDTH/(double)MNT_HEIGHT;}
	
	public int getScreenWidth() {return SCR_WIDTH;}
	public int getScreenHeight() {return SCR_HEIGHT;}
	public double getScreenRatio() {return (double)SCR_WIDTH/(double)SCR_HEIGHT;}
	
	public boolean isFullScreen() {return (glfwGetWindowMonitor(window) == glfwGetPrimaryMonitor());}
	public boolean isScreenCrop() {return SCR_CROP;}
	public boolean isVSync() {return !(SWAP_INTERVAL == 0);}
	
	public boolean isVisible() {return (glfwGetWindowAttrib(window, GLFW_VISIBLE) == GL_TRUE);}
	public boolean isMinimized() {return (glfwGetWindowAttrib(window, GLFW_ICONIFIED) == GL_TRUE);}
	public boolean isFocused() {return (glfwGetWindowAttrib(window, GLFW_FOCUSED) == GL_TRUE);}
	public boolean isResizable() {return (glfwGetWindowAttrib(window, GLFW_RESIZABLE) == GL_TRUE);}
	public boolean isDecorated() {return (glfwGetWindowAttrib(window, GLFW_DECORATED) == GL_TRUE);}
	public boolean isFloating() {return (glfwGetWindowAttrib(window, GLFW_FLOATING) == GL_TRUE);}
	public boolean isAutoMinimize() {return (glfwGetWindowAttrib(window, GLFW_AUTO_ICONIFY) == GL_TRUE);}
	
	public double getTime() {return glfwGetTime();}
	public long getTime(int resolution) {return (long)(glfwGetTime()*Math.pow(10, resolution));}
	public long getTicks() {return ticks;}
	public double getFPS() {return fps;}
	public double getTPS() {return TICKS_PER_SECOND;}
	public double getDeltaTime() {return DELTA_TIME;}
	public int getMaxFrameSkip() {return MAX_FRAME_SKIP;}
	
}
