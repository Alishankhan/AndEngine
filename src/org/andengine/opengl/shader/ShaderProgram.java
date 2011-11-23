package org.anddev.andengine.opengl.shader;

import java.util.HashMap;

import org.anddev.andengine.opengl.shader.exception.ShaderProgramCompileException;
import org.anddev.andengine.opengl.shader.exception.ShaderProgramException;
import org.anddev.andengine.opengl.shader.exception.ShaderProgramLinkException;
import org.anddev.andengine.opengl.shader.source.IShaderSource;
import org.anddev.andengine.opengl.shader.source.StringShaderSource;
import org.anddev.andengine.opengl.shader.util.constants.ShaderProgramConstants;
import org.anddev.andengine.opengl.util.GLState;
import org.anddev.andengine.opengl.vbo.attribute.VertexBufferObjectAttribute;
import org.anddev.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import android.opengl.GLES20;


/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 19:56:34 - 05.08.2011
 */
public class ShaderProgram implements ShaderProgramConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int[] HARDWAREID_CONTAINER = new int[1];
	private static final int[] PARAMETERS_CONTAINER = new int[1];
	private static final int[] LENGTH_CONTAINER = new int[1];
	private static final int[] SIZE_CONTAINER = new int[1];
	private static final int[] TYPE_CONTAINER = new int[1];
	private static final int NAME_CONTAINER_SIZE = 64;
	private static final byte[] NAME_CONTAINER = new byte[ShaderProgram.NAME_CONTAINER_SIZE];

	// ===========================================================
	// Fields
	// ===========================================================

	protected final IShaderSource mVertexShaderSource;
	protected final IShaderSource mFragmentShaderSource;

	protected int mProgramID = -1;

	protected boolean mCompiled;

	protected final HashMap<String, Integer> mUniformLocations = new HashMap<String, Integer>();
	protected final HashMap<String, Integer> mAttributeLocations = new HashMap<String, Integer>();

	// ===========================================================
	// Constructors
	// ===========================================================

	public ShaderProgram(final String pVertexShaderSource, final String pFragmentShaderSource) {
		this(new StringShaderSource(pVertexShaderSource), new StringShaderSource(pFragmentShaderSource));
	}
	
	public ShaderProgram(final IShaderSource pVertexShaderSource, final IShaderSource pFragmentShaderSource) {
		this.mVertexShaderSource = pVertexShaderSource;
		this.mFragmentShaderSource = pFragmentShaderSource;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public boolean isCompiled() {
		return this.mCompiled;
	}

	public void setCompiled(final boolean pCompiled) {
		this.mCompiled = pCompiled;
	}

	public int getAttributeLocation(final String pAttributeName) {
		final Integer location = this.mAttributeLocations.get(pAttributeName);
		if(location != null) {
			return location.intValue();
		} else {
			throw new ShaderProgramException("Unexpected attribute: '" + pAttributeName + "'.");
		}
	}

	public int getAttributeLocationOptional(final String pAttributeName) {
		final Integer location = this.mAttributeLocations.get(pAttributeName);
		if(location != null) {
			return location.intValue();
		} else {
			return ShaderProgramConstants.LOCATION_INVALID;
		}
	}

	public int getUniformLocation(final String pUniformName) {
		final Integer location = this.mUniformLocations.get(pUniformName);
		if(location != null) {
			return location.intValue();
		} else {
			throw new ShaderProgramException("Unexpected uniform: '" + pUniformName + "'.");
		}
	}

	public int getUniformLocationOptional(final String pUniformName) {
		final Integer location = this.mUniformLocations.get(pUniformName);
		if(location != null) {
			return location.intValue();
		} else {
			return ShaderProgramConstants.LOCATION_INVALID;
		}
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void bind(final VertexBufferObjectAttributes pVertexBufferObjectAttributes) throws ShaderProgramException {
		if(!this.mCompiled) {
			this.compile();
		}
		GLState.useProgram(this.mProgramID);

		pVertexBufferObjectAttributes.glVertexAttribPointers();
	}

	public void unbind(final VertexBufferObjectAttributes pVertexBufferObjectAttributes) throws ShaderProgramException {
//		GLES20.glUseProgram(0); // TODO Does this have an positive/negative impact on performance?
	}

	public void delete() {
		if(this.mCompiled) {
			this.mCompiled = false;
			GLState.deleteProgram(this.mProgramID);
			this.mProgramID = -1;
		}
	}

	protected void compile() throws ShaderProgramException {
		final int vertexShaderID = ShaderProgram.compileShader(this.mVertexShaderSource.getShaderSource(), GLES20.GL_VERTEX_SHADER);
		final int fragmentShaderID = ShaderProgram.compileShader(this.mFragmentShaderSource.getShaderSource(), GLES20.GL_FRAGMENT_SHADER);

		this.mProgramID = GLES20.glCreateProgram();
		GLES20.glAttachShader(this.mProgramID, vertexShaderID);
		GLES20.glAttachShader(this.mProgramID, fragmentShaderID);

		this.link();

		GLES20.glDeleteShader(vertexShaderID);
		GLES20.glDeleteShader(fragmentShaderID);
	}

	protected void link() throws ShaderProgramLinkException {
		GLES20.glLinkProgram(this.mProgramID);

		GLES20.glGetProgramiv(this.mProgramID, GLES20.GL_LINK_STATUS, ShaderProgram.HARDWAREID_CONTAINER, 0);
		if(ShaderProgram.HARDWAREID_CONTAINER[0] == 0) {
			throw new ShaderProgramLinkException(GLES20.glGetProgramInfoLog(this.mProgramID));
		}

		this.initAttributeLocations();
		this.initUniformLocations();

		this.mCompiled = true;
	}

	private static int compileShader(final String pSource, final int pType) throws ShaderProgramException {
		final int shaderID = GLES20.glCreateShader(pType);
		if(shaderID == 0) {
			throw new ShaderProgramException("Could not create Shader of type: '" + pType + '"');
		}

		GLES20.glShaderSource(shaderID, pSource);
		GLES20.glCompileShader(shaderID);

		GLES20.glGetShaderiv(shaderID, GLES20.GL_COMPILE_STATUS, ShaderProgram.HARDWAREID_CONTAINER, 0);
		if(ShaderProgram.HARDWAREID_CONTAINER[0] == 0) {
			throw new ShaderProgramCompileException(GLES20.glGetShaderInfoLog(shaderID), pSource);
		}
		return shaderID;
	}

	private void initUniformLocations() {
		this.mUniformLocations.clear();

		ShaderProgram.PARAMETERS_CONTAINER[0] = 0;
		GLES20.glGetProgramiv(this.mProgramID, GLES20.GL_ACTIVE_UNIFORMS, ShaderProgram.PARAMETERS_CONTAINER, 0);
		final int numUniforms = ShaderProgram.PARAMETERS_CONTAINER[0];

		for(int i = 0; i < numUniforms; i++) {
			GLES20.glGetActiveUniform(this.mProgramID, i, ShaderProgram.NAME_CONTAINER_SIZE, ShaderProgram.LENGTH_CONTAINER, 0, ShaderProgram.SIZE_CONTAINER, 0, ShaderProgram.TYPE_CONTAINER, 0, ShaderProgram.NAME_CONTAINER, 0);
			int length = ShaderProgram.LENGTH_CONTAINER[0];
			/* Some drivers do not report the actual length here, but zero. Then the name is '\0' terminated. */
			if(length == 0) {
				while(length < NAME_CONTAINER_SIZE && ShaderProgram.NAME_CONTAINER[length] != '\0') {
					 length++;
				}
			}
			final String name = new String(ShaderProgram.NAME_CONTAINER, 0, length);
			final int location = GLES20.glGetUniformLocation(this.mProgramID, name);
			this.mUniformLocations.put(name, location);
		}
	}

	/**
	 * TODO Is this actually needed? As the locations of {@link VertexBufferObjectAttribute}s are now 'predefined'. 
	 */
	@Deprecated
	private void initAttributeLocations() {
		this.mAttributeLocations.clear();

		ShaderProgram.PARAMETERS_CONTAINER[0] = 0;
		GLES20.glGetProgramiv(this.mProgramID, GLES20.GL_ACTIVE_ATTRIBUTES, ShaderProgram.PARAMETERS_CONTAINER, 0);
		final int numAttributes = ShaderProgram.PARAMETERS_CONTAINER[0];

		for(int i = 0; i < numAttributes; i++) {
			GLES20.glGetActiveAttrib(this.mProgramID, i, ShaderProgram.NAME_CONTAINER_SIZE, ShaderProgram.LENGTH_CONTAINER, 0, ShaderProgram.SIZE_CONTAINER, 0, ShaderProgram.TYPE_CONTAINER, 0, ShaderProgram.NAME_CONTAINER, 0);
			int length = ShaderProgram.LENGTH_CONTAINER[0];
			/* Some drivers do not report the actual length here, but zero. Then the name is '\0' terminated. */
			if(length == 0) {
				while(length < NAME_CONTAINER_SIZE && ShaderProgram.NAME_CONTAINER[length] != '\0') {
					 length++;
				}
			}
			final String name = new String(ShaderProgram.NAME_CONTAINER, 0, length);
			final int location = GLES20.glGetAttribLocation(this.mProgramID, name);
			this.mAttributeLocations.put(name, location);
		}
	}

	public void setUniform(final String pUniformName, final float[] pGLMatrix) {
		GLES20.glUniformMatrix4fv(this.getUniformLocation(pUniformName), 1, false, pGLMatrix, 0);
	}

	public void setUniformOptional(final String pUniformName, final float[] pGLMatrix) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES20.glUniformMatrix4fv(this.getUniformLocationOptional(pUniformName), 1, false, pGLMatrix, 0);
		}
	}

	public void setUniform(final String pUniformName, final float pX) {
		GLES20.glUniform1f(this.getUniformLocation(pUniformName), pX);
	}

	public void setUniformOptional(final String pUniformName, final float pX) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES20.glUniform1f(this.getUniformLocationOptional(pUniformName), pX);
		}
	}

	public void setUniform(final String pUniformName, final float pX, final float pY) {
		GLES20.glUniform2f(this.getUniformLocation(pUniformName), pX, pY);
	}

	public void setUniformOptional(final String pUniformName, final float pX, final float pY) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES20.glUniform2f(this.getUniformLocationOptional(pUniformName), pX, pY);
		}
	}

	public void setUniform(final String pUniformName, final float pX, final float pY, final float pZ) {
		GLES20.glUniform3f(this.getUniformLocation(pUniformName), pX, pY, pZ);
	}

	public void setUniformOptional(final String pUniformName, final float pX, final float pY, final float pZ) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES20.glUniform3f(this.getUniformLocationOptional(pUniformName), pX, pY, pZ);
		}
	}

	public void setUniform(final String pUniformName, final float pX, final float pY, final float pZ, final float pW) {
		GLES20.glUniform4f(this.getUniformLocation(pUniformName), pX, pY, pZ, pW);
	}

	public void setUniformOptional(final String pUniformName, final float pX, final float pY, final float pZ, final float pW) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES20.glUniform4f(this.getUniformLocationOptional(pUniformName), pX, pY, pZ, pW);
		}
	}

	/**
	 * @param pUniformName
	 * @param pTexture the index of the Texture to use. Similar to {@link GLES20#GL_TEXTURE0}, {@link GLES20#GL_TEXTURE1}, ... except that it is <b><code>0</code></b> based.
	 */
	public void setTexture(final String pUniformName, final int pTexture) {
		GLES20.glUniform1i(this.getUniformLocation(pUniformName), pTexture);
	}

	/**
	 * @param pUniformName
	 * @param pTexture the index of the Texture to use. Similar to {@link GLES20#GL_TEXTURE0}, {@link GLES20#GL_TEXTURE1}, ... except that it is <b><code>0</code></b> based.
	 */
	public void setTextureOptional(final String pUniformName, final int pTexture) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES20.glUniform1i(location, pTexture);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}