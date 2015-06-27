package zan.lib.sample;

import zan.lib.core.CoreEngine;
import static zan.lib.input.InputManager.*;

public class SampleCore extends CoreEngine {
	
	@Override
	protected void onKey(int key, int state, int mods, int scancode) {
		if (key == IM_KEY_ESCAPE && state == IM_RELEASE) close();
		else if (key == IM_KEY_F11 && state == IM_RELEASE) toggleFullScreen();
		super.onKey(key, state, mods, scancode);
	}
	
	public static void main(String[] args) {
		SampleCore core = new SampleCore();
		core.setTitle("Sample Test");
		core.setPanel(new TextPanel(core));
		core.run();
	}
	
}
