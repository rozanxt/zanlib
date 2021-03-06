package zan.lib.sample;

import static zan.lib.input.InputManager.*;
import zan.lib.core.BasePanel;
import zan.lib.gfx.obj.ModelObject;
import zan.lib.gfx.shader.DefaultShader;
import zan.lib.gfx.view.ViewPort3D;
import zan.lib.input.InputManager;
import zan.lib.util.math.Vec3D;

public class ModelPanel extends BasePanel {

	private SampleCore core;

	private DefaultShader shader;
	private ViewPort3D viewPort;

	private ModelObject model;

	private Vec3D rotation;

	public ModelPanel(SampleCore core) {
		this.core = core;
	}

	@Override
	public void init() {
		shader = new Sample3DShader();
		shader.loadProgram();
		shader.setClearColor(0.2, 0.2, 0.2, 1.0);
		shader.enableDepthTest(true);
		shader.enableCullFace(true);

		viewPort = new ViewPort3D(0, 0, core.getScreenWidth(), core.getScreenHeight());
		viewPort.setOffsetZ(5.0);
		viewPort.showView();
		viewPort.projectView(shader);

		model = new ModelObject("res/obj/sample_model.obj");

		rotation = new Vec3D(0.0, 0.0, 0.0);
	}

	@Override
	public void destroy() {
		model.destroy();
		shader.destroy();
	}

	@Override
	public void update(double time) {
		if (InputManager.isKeyReleased(InputManager.IM_KEY_ESCAPE)) core.close();
		else if (InputManager.isKeyReleased(InputManager.IM_KEY_F11)) core.toggleFullScreen();

		if (isKeyDown(IM_KEY_W)) rotation.addX(5.0);
		if (isKeyDown(IM_KEY_S)) rotation.subX(5.0);
		if (isKeyDown(IM_KEY_D)) rotation.addY(5.0);
		if (isKeyDown(IM_KEY_A)) rotation.subY(5.0);
	}

	@Override
	public void render(double ip) {
		shader.bind();
		viewPort.adjustView(shader);

		shader.pushMatrix();
		shader.rotate(rotation.getY(), 0.0, 1.0, 0.0);
		shader.rotate(rotation.getX(), 1.0, 0.0, 0.0);
		shader.translate(0.0, -1.5, 0.0);
		shader.applyModelMatrix();
		model.render(shader);
		shader.popMatrix();

		shader.unbind();
	}

	@Override
	public void onScreenResize(int width, int height) {
		shader.bindState();
		viewPort.setScreenSize(width, height);
		viewPort.setViewPort(0, 0, width, height);
		viewPort.showView();
		viewPort.projectView(shader);
	}

}
