package zan.lib.gfx;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;

import zan.lib.math.matrix.Mat44D;
import zan.lib.math.matrix.MatUtil;
import zan.lib.math.vector.Vec4D;
import zan.lib.util.Utility;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
	
	private int programID;
	
	private int vertexPosID;
	private int texCoordID;
	private int texUnitID;
	private int enableTextureID;
	private int colorID;
	private int projectionMatrixID;
	private int modelViewMatrixID;
	
	private Vec4D color;
	
	private ArrayList<Mat44D> matrixStack;
	private Mat44D projectionMatrix;
	private Mat44D modelViewMatrix;
	
	private boolean enableDepthTest;
	private boolean enableBlend;
	private boolean enableTexture;
	
	private FloatBuffer floatBuffer;
	
	public ShaderProgram(String vertexShaderPath, String fragmentShaderPath) {
		programID = 0;
		vertexPosID = 0;
		texCoordID = 0;
		texUnitID = 0;
		enableTextureID = 0;
		colorID = 0;
		projectionMatrixID = 0;
		modelViewMatrixID = 0;
		
		color = new Vec4D(1.0);
		
		matrixStack = new ArrayList<Mat44D>();
		matrixStack.add(MatUtil.identityMat44D());
		projectionMatrix = MatUtil.identityMat44D();
		modelViewMatrix = MatUtil.identityMat44D();
		
		floatBuffer = BufferUtils.createFloatBuffer(16);
		
		loadProgram(vertexShaderPath, fragmentShaderPath);
		
		bind();
		enableDepthTest(false);
		enableBlend(true);
		enableTexture(false);
		setTextureUnit(0);
		unbind();
	}
	
	public void destroy() {
		glDeleteShader(programID);
		programID = 0;
	}
	
	public void bind() {glUseProgram(programID);}
	public void unbind() {glUseProgram(0);}
	
	public void enableDepthTest(boolean depthTest) {
		enableDepthTest = depthTest;
		if (enableDepthTest) glEnable(GL_DEPTH_TEST);
		else glDisable(GL_DEPTH_TEST);
	}
	public void enableBlend(boolean blend) {
		enableBlend = blend;
		if (enableBlend) {
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		} else glDisable(GL_BLEND);
	}
	public void enableTexture(boolean texture) {
		enableTexture = texture;
		if (enableTexture) glUniform1i(enableTextureID, GL_TRUE);
		else glUniform1i(enableTextureID, GL_FALSE);
	}
	
	public void bindState() {
		bind();
		enableDepthTest(enableDepthTest);
		enableBlend(enableBlend);
		enableTexture(enableTexture);
	}
	
	public void enableVertexPointer() {glEnableVertexAttribArray(vertexPosID);}
	public void disableVertexPointer() {glDisableVertexAttribArray(vertexPosID);}
	public void setVertexPointer(int size, int stride, long offset) {glVertexAttribPointer(vertexPosID, size, GL_FLOAT, false, stride*4, offset*4);}
	
	public void enableTexCoordPointer() {glEnableVertexAttribArray(texCoordID);}
	public void disableTexCoordPointer() {glDisableVertexAttribArray(texCoordID);}
	public void setTexCoordPointer(int size, int stride, long offset) {glVertexAttribPointer(texCoordID, size, GL_FLOAT, false, stride*4, offset*4);}
	
	public void setTextureUnit(int texUnit) {glUniform1i(texUnitID, texUnit);}
	
	public void setColor(double r, double g, double b, double a) {color.setComponents(r, g, b, a);}
	public void bindColor() {glUniform4f(colorID, (float)color.get(0), (float)color.get(1), (float)color.get(2), (float)color.get(3));}
	
	public void setProjection(Mat44D matrix) {projectionMatrix.set(matrix);}
	public void applyProjection() {setProjection(getStackMatrix());}
	public void bindProjection() {
		floatBuffer.clear();
		for (int i=0;i<16;i++) floatBuffer.put((float)projectionMatrix.get(i));
		floatBuffer.flip();
		glUniformMatrix4(projectionMatrixID, false, floatBuffer);
	}
	
	public void setModelView(Mat44D matrix) {modelViewMatrix.set(matrix);}
	public void applyModelView() {setModelView(getStackMatrix());}
	public void bindModelView() {
		floatBuffer.clear();
		for (int i=0;i<16;i++) floatBuffer.put((float)modelViewMatrix.get(i));
		floatBuffer.flip();
		glUniformMatrix4(modelViewMatrixID, false, floatBuffer);
	}
	
	public void pushMatrix() {
		if (matrixStack.isEmpty()) matrixStack.add(MatUtil.identityMat44D());
		else matrixStack.add(new Mat44D(getStackMatrix()));
	}
	public void popMatrix() {
		if (!matrixStack.isEmpty()) matrixStack.remove(getStackMatrix()); 
	}
	public void setMatrix(Mat44D matrix) {
		getStackMatrix().set(matrix);
	}
	public void multMatrix(Mat44D matrix) {
		Mat44D stackMatrix = getStackMatrix();
		MatUtil.mult(new Mat44D(stackMatrix), matrix, stackMatrix);
	}
	
	public Mat44D getStackMatrix() {return matrixStack.get(matrixStack.size()-1);}
	public Mat44D getProjectionMatrix() {return projectionMatrix;}
	public Mat44D getModelViewMatrix() {return modelViewMatrix;}
	
	private boolean loadProgram(String vertexShaderPath, String fragmentShaderPath) {
		programID = glCreateProgram();
		loadShaderFile(vertexShaderPath, GL_VERTEX_SHADER);
		loadShaderFile(fragmentShaderPath, GL_FRAGMENT_SHADER);
		glLinkProgram(programID);
		
		if (glGetProgrami(programID, GL_LINK_STATUS) != 1) {
			System.err.println("Error linking GLSL program " + programID + ":\n" + glGetProgramInfoLog(programID));
			destroy();
			return false;
		}
		
		vertexPosID = glGetAttribLocation(programID, "vertexPos");
		if (vertexPosID == -1) System.err.println("vertexPos" + " is not a valid GLSL program variable!");
		texCoordID = glGetAttribLocation(programID, "texCoord");
		if (texCoordID == -1) System.err.println("texCoord" + " is not a valid GLSL program variable!");
		texUnitID = glGetUniformLocation(programID, "texUnit");
		if (texUnitID == -1) System.err.println("texUnit" + " is not a valid GLSL program variable!");
		enableTextureID = glGetUniformLocation(programID, "enableTexture");
		if (enableTextureID == -1) System.err.println("enableTexture" + " is not a valid GLSL program variable!");
		colorID = glGetUniformLocation(programID, "color");
		if (colorID == -1) System.err.println("color" + " is not a valid GLSL program variable!");
		projectionMatrixID = glGetUniformLocation(programID, "projectionMatrix");
		if (projectionMatrixID == -1) System.err.println("projectionMatrix" + " is not a valid GLSL program variable!");
		modelViewMatrixID = glGetUniformLocation(programID, "modelViewMatrix");
		if (modelViewMatrixID == -1) System.err.println("modelViewMatrix" + " is not a valid GLSL program variable!");
		
		return true;
	}
	
	private boolean loadShaderFile(String path, int shaderType) {
		int shader = glCreateShader(shaderType);
		glShaderSource(shader, Utility.readFileAsString(path));
		glCompileShader(shader);
		
		if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
			System.err.println("Unable to compile GLSL shader " + shader + ":\n" + glGetShaderInfoLog(shader));
			return false;
		}
		
		glAttachShader(programID, shader);
		glDeleteShader(shader);
		return true;
	}
	
	public int getProgramID() {return programID;}
	
}
