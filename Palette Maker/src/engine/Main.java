package engine;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
 
/**
 * Basic Main class that is a JFrame and organized like a
 * singleton. Stripped down version from one of my past
 * project's Main class that uses this as the backbone for
 * simple java games.
 * 
 * Simply creates an Application object and calls its 
 * update and render functions. The real meat and potatoes
 * are in the Application class.
 *
 * @author Jello
 */

public class Main extends JFrame implements KeyListener {
	
	/*
	 * Singleton instance
	 */
	private static Main instance = null; 
	
	/*
	 * Instance variables
	 */
	private Dimension screenDimensions;
	private boolean isRunning;
	private Application app;
		
	
	/*
	 * Initiatlization
	 */
	private Main() {
		// Instance variables
		screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
		
		// Listener setup
		addKeyListener(this);
		setSize(screenDimensions.width, screenDimensions.height);
		setUndecorated(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		
		// Run setup
		app = new Application();
		isRunning = true;
	}
	
	public static Main get() {
		if (instance == null) {
			instance = new Main();
		}
		
		// Return single instance
		return instance;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Main loop
	
	public void run() {
		while (isRunning) {
			// Update
			app.update();
			// Render
			render();
			
		}
	}

	public void render() {
		// Creates 2-buffer buffer strategy
		BufferStrategy bs = getBufferStrategy();
		if (getBufferStrategy() == null) {
			createBufferStrategy(2);
			return;
		}

		// Draws to hidden buffer, clears previous image
		Graphics2D g = (Graphics2D) bs.getDrawGraphics();
		g.setBackground(Color.getHSBColor(0.0f, 0.0f, 0.0f));
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.clearRect(0, 0, screenDimensions.width, screenDimensions.height);
		
		// GameState render
		app.render(g);
		
		// Disposes graphics object and shows hidden buffer
		g.dispose();
		bs.show();
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Main
	
	public static void main(String[] args) {
		get().run();
		System.exit(0);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Control

	@Override
	public void keyPressed(KeyEvent e) {
		// End program
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		app.keyPressed(e);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		
	}


}