package engine;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Application {
	
	/**
	 * Resolution of each swatch in the exported image.
	 */
	private static final int EXPORT_RESOLUTION = 16;

	/**
	 * Different methods of deriving the hues based on the
	 * desired amount of hues.
	 * 
	 * <p> LINEAR - Equidistant hues
	 * 
	 * <p> RADIAL - Loosly based off a chunk of the unit circle
	 */
	enum HueStyle {
		LINEAR,
		RADIAL
	}
	
	/**
	 * Style of how raw colors are finalized before exporting
	 * 
	 * <p> BASIC - No extra changes to the raw colors
	 * 
	 * <p> PAIRWISE GRADIENT
	 * 		 - Each swatch takes an increasing
	 * 		   weighted average (based on swatch count)
	 * 		   of the next swatch column.
	 * 
	 * <p> INVERSE PAIRWISE GRADIENT
	 * 		 - Each swatch takes an increasing
	 * 		   weighted average (based on swatch count)
	 * 		   of the previous swatch column.
	 */
	enum RenderStyle {
		BASIC,
		PAIRWISE_GRADIENT,
		INVERSE_PAIRWISE_GRADIENT,
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Variables

	// Instance variables
	private final Dimension SCREEN = Toolkit.getDefaultToolkit().getScreenSize();
	private final int swatchRes = SCREEN.width / 32;

	ApplicationState state;
	int selectionVal;
	int selectionMin;
	int selectionMax;

	// State-controlled variables
	HueStyle hueStyle = HueStyle.LINEAR;
	float hueOffset = 0.0f;
	int hueCount = 1;
	float[] hues;
	int valueCount;
	float[] valueIDs;
	float saturationAdjustment;
	float brightnessAdjustment;
	double rTint, gTint, bTint;
	Color[][] rawColors;
	RenderStyle renderStyle = RenderStyle.BASIC;
	Color[][] finalColors;
	
	// Control variables
	boolean rightArrowQueued = false;
	boolean leftArrowQueued = false;
	boolean upArrowQueued = false;
	boolean downArrowQueued = false;
	boolean enterQueued = false;
	char selectedColor = 'r';

	///////////////////////////////////////////////////////////////////////////////
	//
	// Construction

	/**
	 * Creates Application and sets to the first state
	 * with the appropiate minimum/maximum selection options.
	 */
	public Application() {
		state = ApplicationState.PICK_HUE_STYLE;
		selectionVal = 0;
		selectionMin = 0;
		selectionMax = HueStyle.values().length - 1;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// Update
	/**
	 * Called every frame before render(). Executes the actions
	 * of the queued controls. Adjusts hues for certain states.
	 */
	public void update() {
		// Update the queued controls
		queuedControlsUpdate();
		// Check if for hue reevaluation
		reevaluateHues();
	}
	
	/**
	 * Executes the actions of queued control buttons. Done to avoid
	 * concurrently modifying anything wiith the Keylistener thread.
	 */
	private void queuedControlsUpdate() {
		// Right arrow
		if (rightArrowQueued) {
			// Right arrow functions
			if (state == ApplicationState.ADJUST_TINTS) {
				switch (selectedColor) {
				case 'r':
					rTint += 5f;
					rTint = (rTint > 255) ? 255 : rTint;
					rTint = (rTint < 0) ? 0 : rTint;
					break;
				case 'g':
					gTint += 5f;
					gTint = (gTint > 255) ? 255 : gTint;
					gTint = (gTint < 0) ? 0 : gTint;
					break;
				case 'b':
					bTint += 5f;
					bTint = (bTint > 255) ? 255 : bTint;
					bTint = (bTint < 0) ? 0 : bTint;
					break;
				}
			} else {
				selectionVal = (selectionVal + 1 > selectionMax) ? selectionMax : selectionVal + 1;
			}
			// Increment boolean
			rightArrowQueued = false;
		}

		// Left arrow
		if (leftArrowQueued) {
			// Left arrow functions
			if (state == ApplicationState.ADJUST_TINTS) {
				switch (selectedColor) {
				case 'r':
					rTint -= 5f;
					break;
				case 'g':
					gTint -= 5f;
					break;
				case 'b':
					bTint -= 5f;
					break;
				}
			} else {
				selectionVal = (selectionVal - 1 < selectionMin) ? selectionMin : selectionVal - 1;
			}
			// Increment boolean
			leftArrowQueued = false;
		}

		// Up arrow
		if (upArrowQueued) {
			// Up arrow functions
			if (state == ApplicationState.ADJUST_TINTS) {
				switch (selectedColor) {
				case 'r':
					selectedColor = 'g';
					break;
				case 'g':
					selectedColor = 'b';
					break;
				case 'b':
					selectedColor = 'r';
					break;
				}
			} else {
				modifyHueOffset();
			}
			// Increment boolean
			upArrowQueued = false;
		}

		// Down arrow
		if (downArrowQueued) {
			// Down arrow functions
			if (state == ApplicationState.ADJUST_TINTS) {
				switch (selectedColor) {
				case 'r':
					selectedColor = 'b';
					break;
				case 'g':
					selectedColor = 'r';
					break;
				case 'b':
					selectedColor = 'g';
					break;
				}
			} else {
				// Up arrow functions
				modifyHueOffset();
			}
			// Increment boolean
			downArrowQueued = false;
		}

		// Enter
		if (enterQueued) {
			// Enter functions
			incrementState();
			// Increment boolean
			enterQueued = false;
		}
	}

	/**
	 * Called during the PICK_HUES state. Recalculates the
	 * hues whenever the user changes how many hues they want to use.
	 */
	private  void reevaluateHues() {
		if (state == ApplicationState.PICK_HUES && hues.length != selectionVal) {
			switch (hueStyle) {
			case LINEAR:
				hues = new float[selectionVal];
				float hueStep = 1.0f / ((float) selectionVal);
				for (int i = 0; i < selectionVal; i++) {
					hues[i] = hueStep * i + hueOffset;
				}
				break;
			case RADIAL:
				hues = new float[selectionVal];
				for (int i = 0; i < selectionVal; i++) {
					float x = ((float) i) / ((float) selectionVal);
					float a = (float) Math.sqrt(1 - (x * x));
					float b = (1.0f - x);
					hues[i] = (a + b) * 0.5f + hueOffset;
				}
				break;
			}
		}
	}
	
	/**
	 * Adjusts the main hues offset. Adjusts the hues directly but
	 * saves the total hueOffset in case the hues are recalculated.
	 */
	private void modifyHueOffset() {
		if (state.ordinal() > ApplicationState.PICK_HUE_STYLE.ordinal()) {
			float hueStep = 0.025f;
			if (upArrowQueued) {
				for (int i = 0; i < hues.length; i++) {
					hues[i] += hueStep;
				}
				hueOffset += hueStep;
			}
			if (downArrowQueued) {
				for (int i = 0; i < hues.length; i++) {
					hues[i] -= hueStep;
				}
				hueOffset -= hueStep;
			}
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Render

	/**
	 * Renders all the aspects of the applicaiton in their not too specific order.
	 */
	public void render(Graphics2D g) {
		renderBackground(g);
		renderPrompt(g);
		renderSelection(g);
		renderVisuals(g);
		renderControls(g);
	}
	
	/**
	 * Renders a neutral gray background rectangle to give the least
	 * amount of visual noise while creating the palettes.
	 */
	private void renderBackground(Graphics2D g) {
		// Neutral background
		g.setColor(Color.getHSBColor(0.0f, 0.0f, 0.5f));
		g.fillRect(0, 0, SCREEN.width, SCREEN.height);
	}

	/**
	 * Renders a message based off of the state that tells the user what values they are modifying.
	 */
	private void renderPrompt(Graphics2D g) {
		g.setFont(new Font("Dialogue", Font.PLAIN, swatchRes)); 
		// Render prompt
		String promptText;
		switch (state) {
		case PICK_HUE_STYLE:
			promptText = "Which style of palette derivation?";
			break;
		case PICK_HUES:
			promptText = "How many different hues?";
			break;
		case PICK_VALUE_COUNT:
			promptText = "How many swatches for each hue?";
			break;
		case ADJUST_SATURATION:
			promptText = "Adjust the saturation as needed:";
			break;
		case ADJUST_BRIGHTNESS:
			promptText = "Adjust the brightness as needed: ";
			break;
		case ADJUST_TINTS:
			promptText = "Adjust RGB tint";
			break;
		case PICK_RENDER_STYLE:
			promptText = "Which style of rendering finalization?";
			break;
		default:
			promptText = "Default prompt text";
			break;
		}
		int promptTextWidth = g.getFontMetrics().stringWidth(promptText);
		g.setColor(Color.white);
		g.drawString(promptText, SCREEN.width / 2 - promptTextWidth / 2, SCREEN.height / 9);
	}
	
	/**
	 * Renders a message based off of the state that tells the user to what extend they are modifying the values.
	 */
	private void renderSelection(Graphics2D g) {
		g.setFont(new Font("Dialogue", Font.PLAIN, swatchRes / 2)); 
		g.setColor(Color.white);
		// Render selection
		String selectionText;
		switch (state) {
		case PICK_HUE_STYLE:
			selectionText = "Selected style: " + HueStyle.values()[selectionVal];
			break;
		case PICK_HUES:
			selectionText = "Hue count: " + selectionVal;
			break;
		case PICK_VALUE_COUNT:
			selectionText = "Value swatch count: " + selectionVal;
			break;
		case ADJUST_SATURATION:
			selectionText = "Saturation level: " + (selectionVal * 10) + "%";
			break;
		case ADJUST_BRIGHTNESS:
			selectionText = "Brightness level: " + (selectionVal * 10) + "%";
			break;
		case ADJUST_TINTS:
			selectionText = "";
			switch (selectedColor) {
			case 'r':
				g.setColor(Color.red);
				selectionText = "Red tint level: " + (rTint / 1.0) + "%";
				break;
			case 'g':
				g.setColor(Color.green);
				selectionText = "Green tint level: " + (gTint / 1.0) + "%";
				break;
			case 'b':
				g.setColor(Color.blue);
				selectionText = "Blue tint level: " + (bTint / 1.0) + "%";
				break;
			}
			break;
		case PICK_RENDER_STYLE:
			selectionText = "Selected  style: " + RenderStyle.values()[selectionVal];
			break;
		default:
			selectionText = "";
			break;
		}
		int selectionTextWidth = g.getFontMetrics().stringWidth(selectionText);
		g.drawString(selectionText, SCREEN.width / 2 - selectionTextWidth / 2,
				SCREEN.height / 9 + swatchRes);
	}

	/**
	 * Renders a message based off of the state that tells the user what controls to use to modify the values.
	 */
	private void renderControls(Graphics2D g) {
		String controlText;
		switch (state) {
		case PICK_HUE_STYLE:
		case PICK_HUES:
		case PICK_VALUE_COUNT:
		case ADJUST_SATURATION:
		case ADJUST_BRIGHTNESS:
		case PICK_RENDER_STYLE:
			controlText = "Use LEFT / RIGHT arrows to adjust. Press ENTER to submit.";
			break;
		case ADJUST_TINTS:
			controlText = ">>>> Use UP / DOWN to cycle RGB. Use LEFT / RIGHT arrows to adjust. Press ENTER to submit. <<<<";
			break;
		default:
			controlText = "";
			break;
		}
		g.setColor(Color.white);
		int controlsTextWidth = g.getFontMetrics().stringWidth(controlText);
		g.drawString(controlText, SCREEN.width / 2 - controlsTextWidth / 2,
				SCREEN.height - swatchRes);
	}
	
	/**
	 * Renders a visual representation of the current palette based off of the state and currently selected values.
	 */
	private void renderVisuals(Graphics2D g) {
		int res = swatchRes;
		if (state == ApplicationState.PICK_HUES) {
			// By hues
			int offset = selectionVal * res / 2;
			for (int i = 0; i < selectionVal; i++) {
				g.setColor(Color.getHSBColor(hues[i], 1.0f, 1.0f));
				g.fillRect(SCREEN.width / 2 - offset + i * res, SCREEN.height / 2, res, res);
			}
		}
		
		if (state == ApplicationState.PICK_VALUE_COUNT) {
			// By values
			float[] valIDs = deriveValueId();
			for (int j = 0; j < valIDs.length; j++) {
				int verticalOffset = selectionVal * res / 2;
				int individualVerticalOffset = -j * res;
				// By hues
				int offset = hues.length * res / 2;
				for (int i = 0; i < hues.length; i++) {
					float valueID = valIDs[j];
					if (valueID > 1.0f) {
						g.setColor(Color.getHSBColor(hues[i], 2.0f - valIDs[j], 1.0f));
					} else {
						g.setColor(Color.getHSBColor(hues[i], 1.0f, valIDs[j]));
					}
					g.fillRect(SCREEN.width / 2 - offset + i * res, SCREEN.height / 2 + individualVerticalOffset + verticalOffset, res, res);
				}
			}
		}
		
		if (state == ApplicationState.ADJUST_SATURATION) {
			float saturationAdjustment = ((float) selectionVal) / 10.0f;
			// By values
			for (int j = 0; j < valueIDs.length; j++) {
				int verticalOffset = valueCount * res / 2;
				int individualVerticalOffset = -j * res;
				// By hues
				int offset = hues.length * res / 2;
				for (int i = 0; i < hues.length; i++) {
					float valueID = valueIDs[j];
					if (valueID > 1.0f) {
						g.setColor(Color.getHSBColor(hues[i], (2.0f - valueIDs[j]) * saturationAdjustment, 1.0f));
					} else {
						g.setColor(Color.getHSBColor(hues[i], 1.0f * saturationAdjustment, valueIDs[j]));
					}
					g.fillRect(SCREEN.width / 2 - offset + i * res, SCREEN.height / 2 + individualVerticalOffset + verticalOffset, res, res);
				}
			}
		}
		
		if (state == ApplicationState.ADJUST_BRIGHTNESS) {
			float brightnessAdjustment = ((float) selectionVal) / 10.0f;
			// By values
			for (int j = 0; j < valueIDs.length; j++) {
				int verticalOffset = valueCount * res / 2;
				int individualVerticalOffset = -j * res;
				// By hues
				int offset = hues.length * res / 2;
				for (int i = 0; i < hues.length; i++) {
					float valueID = valueIDs[j];
					if (valueID > 1.0f) {
						g.setColor(Color.getHSBColor(hues[i], (2.0f - valueIDs[j]) * saturationAdjustment, 1.0f * brightnessAdjustment));
					} else {
						g.setColor(Color.getHSBColor(hues[i], 1.0f * saturationAdjustment, valueIDs[j] * brightnessAdjustment));
					}
					g.fillRect(SCREEN.width / 2 - offset + i * res, SCREEN.height / 2 + individualVerticalOffset + verticalOffset, res, res);
				}
			}
		}
		
		if (state == ApplicationState.ADJUST_TINTS) {
			// By values
			for (int j = 0; j < valueIDs.length; j++) {
				int verticalOffset = valueCount * res / 2;
				int individualVerticalOffset = -j * res;
				// By hues
				int offset = hues.length * res / 2;
				for (int i = 0; i < hues.length; i++) {
					// Calculate the color just like normal
					float valueID = valueIDs[j];
					Color c;
					if (valueID > 1.0f) {
						c = Color.getHSBColor(hues[i], (2.0f - valueIDs[j]) * saturationAdjustment, 1.0f * brightnessAdjustment);
					} else {
						c = Color.getHSBColor(hues[i], 1.0f * saturationAdjustment, valueIDs[j] * brightnessAdjustment);
					}
					// Adjust RGB values with weighted average with tint
					double redRatio = rTint / 100.0;
					double greenRatio = gTint / 100.0;
					double blueRatio = bTint / 100.0;
					int deltaRed = (int) (redRatio * 255);
					int deltaGreen = (int) (greenRatio * 255);
					int deltaBlue = (int) (blueRatio * 255);
					int newRed = c.getRed();
					int newGreen = c.getGreen();
					int newBlue = c.getBlue();
					// Adjust red tint with weighted aberage
					newRed = deltaRed + (int)(newRed * (1.0 - redRatio));
					newRed = Math.min(newRed, 255);
					newRed = Math.max(newRed, 0);
					newGreen = deltaGreen + (int)(newGreen * (1.0 - redRatio));
					newGreen = Math.min(newGreen, 255);
					newGreen = Math.max(newGreen, 0);
					newBlue = deltaBlue + (int)(newBlue * (1.0 - redRatio));
					newBlue = Math.min(newBlue, 255);
					newBlue = Math.max(newBlue, 0);
					c = new Color(newRed, newGreen, newBlue);
					// Set color and render
					g.setColor(c);
					g.fillRect(SCREEN.width / 2 - offset + i * res, SCREEN.height / 2 + individualVerticalOffset + verticalOffset, res, res);
				}
			}
		}
		
		if (state == ApplicationState.PICK_RENDER_STYLE) {
			switch (RenderStyle.values()[selectionVal]) {
			case BASIC:
				// By values
				for (int j = 0; j < valueIDs.length; j++) {
					int verticalOffset = valueCount * res / 2;
					int individualVerticalOffset = -j * res;
					// By hues
					int offset = hues.length * res / 2;
					for (int i = 0; i < hues.length; i++) {
						g.setColor(rawColors[j][i]);
						g.fillRect(SCREEN.width / 2 - offset + i * res, SCREEN.height / 2 + individualVerticalOffset + verticalOffset, res, res);
					}
				}
				break;
			case PAIRWISE_GRADIENT:
				// By values
				double gradientStep = 1.0 / valueIDs.length;
				for (int j = 0; j < valueIDs.length; j++) {
					int verticalOffset = valueCount * res / 2;
					int individualVerticalOffset = -j * res;
					// By hues
					int offset = hues.length * res / 2;
					for (int i = 0; i < hues.length; i++) {
						int nextIndex = (i == hues.length - 1) ? 0 : i + 1;
						Color currentColor = rawColors[j][i];
						Color nextColor = rawColors[j][nextIndex];
						// Take weighted average of each color
						double gradient = gradientStep * j + gradientStep / 2.0;
						double invGradient = 1 - gradient;
						int wr = (int) ((double) (currentColor.getRed() * gradient) + (double) (nextColor.getRed() * invGradient));
						int wg = (int) ((double) (currentColor.getGreen() * gradient) + (double) (nextColor.getGreen() * invGradient));
						int wb = (int) ((double) (currentColor.getBlue() * gradient) + (double) (nextColor.getBlue() * invGradient));
						g.setColor(new Color (wr, wg, wb));
						g.fillRect(SCREEN.width / 2 - offset + i * res, SCREEN.height / 2 + individualVerticalOffset + verticalOffset, res, res);
					}
				}
				break;
				
			case INVERSE_PAIRWISE_GRADIENT:
				// By values
				gradientStep = 1.0 / valueIDs.length;
				for (int j = 0; j < valueIDs.length; j++) {
					int verticalOffset = valueCount * res / 2;
					int individualVerticalOffset = -j * res;
					// By hues
					int offset = hues.length * res / 2;
					for (int i = 0; i < hues.length; i++) {
						int nextIndex = (i == hues.length - 1) ? 0 : i + 1;
						Color currentColor = rawColors[j][i];
						Color nextColor = rawColors[j][nextIndex];
						// Take weighted average of each color
						double invGradient = gradientStep * j  + gradientStep / 2.0;
						double gradient = 1 - invGradient;
						int wr = (int) ((double) (currentColor.getRed() * gradient) + (double) (nextColor.getRed() * invGradient));
						int wg = (int) ((double) (currentColor.getGreen() * gradient) + (double) (nextColor.getGreen() * invGradient));
						int wb = (int) ((double) (currentColor.getBlue() * gradient) + (double) (nextColor.getBlue() * invGradient));
						g.setColor(new Color (wr, wg, wb));
						g.fillRect(SCREEN.width / 2 - offset + i * res, SCREEN.height / 2 + individualVerticalOffset + verticalOffset, res, res);
					}
				}
				break;
			}
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Control

	/**
	 * Queues the button presses to be executed in the update function.
	 */
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_RIGHT:
			rightArrowQueued = true;
			break;
		case KeyEvent.VK_LEFT:
			leftArrowQueued = true;
			break;
		case KeyEvent.VK_UP:
			upArrowQueued = true;
			break;
		case KeyEvent.VK_DOWN:
			downArrowQueued = true;
			break;
		case KeyEvent.VK_ENTER:
			enterQueued = true;
			break;
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Misc

	private void incrementState() {
		switch (state) {
		case PICK_HUE_STYLE:
			// State specific change
			hueStyle = HueStyle.values()[selectionVal];
			hues = new float[] {0.0f};
			// Increment
			state = ApplicationState.PICK_HUES;
			selectionMin = 1;
			selectionMax = 28;
			selectionVal = selectionMin;
			break;
		case PICK_HUES:
			// Increment
			state = ApplicationState.PICK_VALUE_COUNT;
			selectionMin = 3;
			selectionMax = 8;
			selectionVal = selectionMin;
			break;
		case PICK_VALUE_COUNT:
			// State specific change
			valueIDs = deriveValueId();
			valueCount = selectionVal;
			// Increment
			state = ApplicationState.ADJUST_SATURATION;
			selectionMin = 1;
			selectionMax = 10;
			selectionVal = 10;
			break;
		case ADJUST_SATURATION:
			// State specific change
			saturationAdjustment = ((float) selectionVal) / 10.0f;
			// Increment
			state = ApplicationState.ADJUST_BRIGHTNESS;
			selectionMin = 1;
			selectionMax = 10;
			selectionVal = 10;
			break;
		case ADJUST_BRIGHTNESS:
			// State specific change
			brightnessAdjustment = ((float) selectionVal) / 10.0f;
			// Increment
			state = ApplicationState.ADJUST_TINTS;
			break;
		case ADJUST_TINTS:
			// State specific change
			rawColors = new Color[valueIDs.length][hues.length];
			finalizeTints();
			// Increment
			state = ApplicationState.PICK_RENDER_STYLE;
			selectionVal = 0;
			selectionMin = 0;
			selectionMax = RenderStyle.values().length - 1;
			break;
		case PICK_RENDER_STYLE:
			// State specific change
			renderStyle = RenderStyle.values()[selectionVal];
			finalizeColors();
			exportPalette();
			break;
		}

	}

	/**
	 * Called after the last application state. Turns the rawColors
	 * color array into an image file to be exported. If the user has
	 * a folder on their desktop named "palettes", the image will be
	 * exported there. Otherwise the image will be exported to the desktop.
	 */
	private void exportPalette() {
		int res = EXPORT_RESOLUTION;
		int height = valueIDs.length * res;
		int width = hues.length * res;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		for (int j = 0; j < valueIDs.length; j++) {
			for (int i = 0; i < hues.length; i++) {
				int xOff = i * res;
				int yOff = j * res;
				for (int y = 0; y < res; y++) {
					for (int x = 0; x < res; x++) {
						image.setRGB(xOff + x, yOff + y, finalColors[j][i].getRGB());
					}
				}
			}
		}
		
		try {
			File output;
			File path = new File(System.getProperty("user.home") + "/Desktop/palettes/");
			if (path.exists() && path.isDirectory()) {
				output = new File(System.getProperty("user.home") + "/Desktop/palettes/" + "" + "palette-0.png");
			} else {
				output = new File(System.getProperty("user.home") + "/Desktop/" + "" + "palette-0.png");
			}
			ImageIO.write(image, "png", output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("outputted image");
		System.exit(0);
	}
	
	/** 
	 * Combines all previous calculations to translate the abstract 
	 * into concrete colors into an array of Colors, rawColors.
	 */
	private void finalizeTints() {
		for (int j = 0; j < valueIDs.length; j++) {
			// By hues
			for (int i = 0; i < hues.length; i++) {
				// Calculate the color just like normal
				float valueID = valueIDs[j];
				Color c;
				if (valueID > 1.0f) {
					c = Color.getHSBColor(hues[i], (2.0f - valueIDs[j]) * saturationAdjustment, 1.0f * brightnessAdjustment);
				} else {
					c = Color.getHSBColor(hues[i], 1.0f * saturationAdjustment, valueIDs[j] * brightnessAdjustment);
				}
				// Adjust RGB values with weighted average with tint
				double redRatio = rTint / 100.0;
				double greenRatio = gTint / 100.0;
				double blueRatio = bTint / 100.0;
				int deltaRed = (int) (redRatio * 255);
				int deltaGreen = (int) (greenRatio * 255);
				int deltaBlue = (int) (blueRatio * 255);
				int newRed = c.getRed();
				int newGreen = c.getGreen();
				int newBlue = c.getBlue();
				// Adjust red tint with weighted aberage
				newRed = deltaRed + (int)(newRed * (1.0 - redRatio));
				newRed = Math.min(newRed, 255);
				newRed = Math.max(newRed, 0);
				newGreen = deltaGreen + (int)(newGreen * (1.0 - redRatio));
				newGreen = Math.min(newGreen, 255);
				newGreen = Math.max(newGreen, 0);
				newBlue = deltaBlue + (int)(newBlue * (1.0 - redRatio));
				newBlue = Math.min(newBlue, 255);
				newBlue = Math.max(newBlue, 0);
				c = new Color(newRed, newGreen, newBlue);
				// Set color to raw colors
				rawColors[j][i] = c;
			}
		}
	}
	
	/** 
	 * Does any extra modifications to the raw colors or just leaves them be.
	 * Moves modified/unmodified colors into the finalColors array. Last changes
	 * to the colors before being exportation.
	 */
	private void finalizeColors() {
		finalColors = new Color[valueIDs.length][hues.length];
		
		switch (renderStyle) {
		case BASIC:
			for (int j = 0; j < valueIDs.length; j++) {
				for (int i = 0; i < hues.length; i++) {
					finalColors[j][i] = rawColors[j][i];
				}
			}
			break;
		case PAIRWISE_GRADIENT:
			// By values
			double gradientStep = 1.0 / valueIDs.length;
			for (int j = 0; j < valueIDs.length; j++) {
				// By hues
				for (int i = 0; i < hues.length; i++) {
					int nextIndex = (i == hues.length - 1) ? 0 : i + 1;
					Color currentColor = rawColors[j][i];
					Color nextColor = rawColors[j][nextIndex];
					// Take weighted average of each color
					double gradient = gradientStep * j + gradientStep / 2.0;
					double invGradient = 1 - gradient;
					int wr = (int) ((double) (currentColor.getRed() * gradient) + (double) (nextColor.getRed() * invGradient));
					int wg = (int) ((double) (currentColor.getGreen() * gradient) + (double) (nextColor.getGreen() * invGradient));
					int wb = (int) ((double) (currentColor.getBlue() * gradient) + (double) (nextColor.getBlue() * invGradient));
					finalColors[j][i] = new Color (wr, wg, wb);
				}
			}
			break;
		case INVERSE_PAIRWISE_GRADIENT:
			// By values
			gradientStep = 1.0 / valueIDs.length;
			for (int j = 0; j < valueIDs.length; j++) {
				// By hues
				for (int i = 0; i < hues.length; i++) {
					int nextIndex = (i == hues.length - 1) ? 0 : i + 1;
					Color currentColor = rawColors[j][i];
					Color nextColor = rawColors[j][nextIndex];
					// Take weighted average of each color
					double invGradient = gradientStep * j  + gradientStep / 2.0;
					double gradient = 1 - invGradient;
					int wr = (int) ((double) (currentColor.getRed() * gradient) + (double) (nextColor.getRed() * invGradient));
					int wg = (int) ((double) (currentColor.getGreen() * gradient) + (double) (nextColor.getGreen() * invGradient));
					int wb = (int) ((double) (currentColor.getBlue() * gradient) + (double) (nextColor.getBlue() * invGradient));
					finalColors[j][i] = new Color (wr, wg, wb);
				}
			}
			break;
		}
	}
	
	/**
	 * Called by the PICK_VALUE_COUNT state. Calculates the "valueID" 
	 * of each swatch based on the selected amount. 
	 * 
	 * <p> "valueID"'s are my representation of value between 0.0f and 2.0f.
	 * <p> Where 0.0f is black, where 1.0f is red (for example), and where 2.0 is white
	 * 
	 * <p> To go from 1.0f to 0.0f you would decrease the brightness from pure red. 
	 * To go from 1.0f to 2.0f you would increase the saturation from pure red
	 */
	private float[] deriveValueId() {
		float[] values = new float[selectionVal]; 
		float valueStep = 1.0f / ((float) selectionVal - 1);
		for (int i = 0; i < selectionVal; i++) {
			values[i] = valueStep * i;
			values[i] *= 2;
		}
		return values;
	}
	
}
