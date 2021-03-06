package core;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

import misc.Config;
import misc.TextLineNumber;
import misc.UpperCaseDocument;

public class MainWindow extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//The currently open file
	String openFile = "";

	//The highlight of the last line executed.  We need this to remove that highlight
	Object previousLineHighlighter;
	
	//The currently highlighted memory address.  We need this to remove that highlight
	JLabel previousMemoryAddress;

	//Stores the labels associated with the registers
	Map<String, JLabel> registerLabels = new HashMap<String, JLabel>();
	
	//Stores the labels associated with main memory
	JLabel[]  mainMemoryLabels = new JLabel[ Config.mainMemoryLength ];

	//Stores the labels associated with the string buffer
	JLabel[]  stringBufferLabels = new JLabel[ Config.stringBufferSize ];
	
	//Keeps track of whether or not the file has been changed
	boolean fileHasChanged = false;

	//Keeps track of whether the code is currently executing
	boolean isRunning = false;

	//The path of the most recently opened file
	//Default to execution path
	File currentFile = new File( System.getProperty("user.dir") );

	//UI Variables
	
	//Font
	Font font;
	
	//Buttons
	JButton saveButton;
	JButton newButton;
	JButton openButton;
	JButton stepButton;
	JButton fastForwardButton;
	JButton runStopButton;

	//Button icons
	ImageIcon runIcon;
	ImageIcon stopIcon;

	//Text area
	JTextArea codeTextArea;
	JTextArea consoleTextArea;

	//Code line counter
	TextLineNumber codeLineNumber;

	//Panel of registers
	JPanel registersPanel;
	
	//Panel for main memory
	JPanel mainMemoryPanel;
	
	//Main memory scroll bar
	JScrollPane mainMemoryPanelScrollPane;
	
	//Panel for the string buffer
	JPanel stringBufferPanel;
	
	//String buffer scroll bar
	JScrollPane stringBufferPanelScrollPane;
	
	//A reference to the processing logic
	private ProcessingLogic logic;
	
	//The thread we'll use while fast forwarding
	Thread fastForwardThread;
	
	public void start(){

		//Get the processing logic reference
		logic = primary.processingLogic;
		
		//Font
		Charset.forName( "UTF-8" );
		font = new Font( "Consolas", Font.PLAIN, Config.fontSize );

		//Main icon
		ImageIcon img = getImageIcon( "icon.png" );
		this.setIconImage( img.getImage() );

		//Make this look good
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}

		//Main window properties
		this.setTitle( Config.titleBase );
		this.setSize( new Dimension( 600, 850 ) );
		this.setMinimumSize( new Dimension( 600, 500 ) );
		this.setLocationByPlatform( true );
		this.setLayout( new BorderLayout() );
		this.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );

		//Confirm close if there are unsaved changes 
		this.addWindowListener( new WindowListener() {

			@Override
			public void windowActivated(WindowEvent arg0) {}

			@Override
			public void windowClosed(WindowEvent arg0) {}

			@Override
			public void windowClosing(WindowEvent arg0) {

				//Ask them if they want to close without saving
				if( fileHasChanged ) {

					int dialogResult = JOptionPane.showConfirmDialog(
							codeTextArea,
							"There are unsaved changes to this file.\n\nWould you like to save before exiting?",
							"Warning",
							JOptionPane.YES_NO_CANCEL_OPTION
							);

					//If they do want to save
					if( dialogResult == JOptionPane.YES_OPTION ){

						//Let them save
						doASave();
					}

					//If they click cancel, pretend they didn't try to exit
					if( dialogResult == JOptionPane.CANCEL_OPTION ){
						return;
					}
				}

				//Having dealt with that, exit the program
				System.exit( 1 );

			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {}

			@Override
			public void windowDeiconified(WindowEvent arg0) {}

			@Override
			public void windowIconified(WindowEvent arg0) {}

			@Override
			public void windowOpened(WindowEvent arg0) {}

		});

		//Text pane
		codeTextArea = new JTextArea();
		codeTextArea.setFont( font );
		codeTextArea.setTabSize( 3 );
		
		//Used to convert typed text into uppercase on the fly
		codeTextArea.setDocument( new UpperCaseDocument() );

		//Listener to catch changes
		codeTextArea.getDocument().addDocumentListener( new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent e) {}

			@Override
			public void insertUpdate(DocumentEvent e) {
				setFileHasChanged( true );
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				if( codeTextArea.getText().length() == 0 ) {
					setFileHasChanged( false );
				}else {
					setFileHasChanged( true );
				}
			}

		});

		//Keyboard listener for ctrl-s
		codeTextArea.addKeyListener( new KeyListener() {

			boolean controlDown = false;

			@Override
			public void keyPressed( KeyEvent event ) {

				//Check for control
				if( event.getKeyCode() == KeyEvent.VK_CONTROL ) {
					controlDown = true;
				}

				//Check for S
				if( event.getKeyCode() == KeyEvent.VK_S ) {

					//Now this is a save
					if( controlDown ) {
						doASave();

						event.consume();
					}
				}

			}

			@Override
			public void keyReleased( KeyEvent event ) {

				//Check for control
				if( event.getKeyCode() == KeyEvent.VK_CONTROL ) {
					controlDown = false;
				}

			}

			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub

			}

		});

		//Code line numbers
		codeLineNumber = new TextLineNumber( codeTextArea );
		codeLineNumber.setFont( font );

		//Code scroll pane
		JScrollPane codeScrollPane = new JScrollPane( codeTextArea );
		codeScrollPane.setRowHeaderView( codeLineNumber );

		//Registers
		registersPanel = new JPanel();
		registersPanel.setLayout( new BoxLayout( registersPanel, BoxLayout.X_AXIS ) );

		//Add special registers
		//Program counter
		addRegister( "PC", 0 );
		
		//Memory read/write head
		addRegister( "MH", 0 );
		
		int specialRegisterCount = registersPanel.getComponentCount();
		
		//Add regular registers
		for (int i = 0; i < Config.registerCount; i++) {
			addRegister( "R" + i, 0 );
		}
		
		registersPanel.setPreferredSize( new Dimension( 50 * ( Config.registerCount + specialRegisterCount ), 50 ) );

		//Registers scroll pane
		JScrollPane registersPanelScrollPane = new JScrollPane( registersPanel );
		registersPanelScrollPane.setMinimumSize( new Dimension( Integer.MAX_VALUE, 75 ) );
		registersPanelScrollPane.setMaximumSize( new Dimension( Integer.MAX_VALUE, 75 ) );
		registersPanelScrollPane.setPreferredSize( new Dimension( Integer.MAX_VALUE, 75 ) );
		registersPanelScrollPane.getHorizontalScrollBar().setUnitIncrement( 16 );
		registersPanelScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_NEVER );
		
		
		//Main memory
		mainMemoryPanel = new JPanel();
		mainMemoryPanel.setLayout( new BoxLayout( mainMemoryPanel, BoxLayout.X_AXIS ) );
		mainMemoryPanel.setPreferredSize( new Dimension( Config.mainMemoryLength * 75, 50 ) );
		
		//Add memory spaces
		for (int i = 0; i < Config.mainMemoryLength; i++) {
			addMainMemorySpace( i, 0 );
		}

		//Main memory scroll pane
		mainMemoryPanelScrollPane = new JScrollPane( mainMemoryPanel );
		mainMemoryPanelScrollPane.setMinimumSize( new Dimension( Integer.MAX_VALUE, 75 ) );
		mainMemoryPanelScrollPane.setMaximumSize( new Dimension( Integer.MAX_VALUE, 75 ) );
		mainMemoryPanelScrollPane.setPreferredSize( new Dimension( Integer.MAX_VALUE, 75 ) );
		mainMemoryPanelScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_NEVER );
		mainMemoryPanelScrollPane.getHorizontalScrollBar().setUnitIncrement( 16 );
		
		//String buffer
		stringBufferPanel = new JPanel();
		stringBufferPanel.setLayout( new BoxLayout( stringBufferPanel, BoxLayout.X_AXIS ) );
		stringBufferPanel.setPreferredSize( new Dimension( Config.stringBufferSize * 50, 50 ) );
		
		//Add buffer spaces
		for (int i = 0; i < Config.stringBufferSize; i++) {
			addStringBufferCharacter( i, "" );
		}

		//String buffer scroll pane
		stringBufferPanelScrollPane = new JScrollPane( stringBufferPanel );
		stringBufferPanelScrollPane.setMinimumSize( new Dimension( Integer.MAX_VALUE, 75 ) );
		stringBufferPanelScrollPane.setMaximumSize( new Dimension( Integer.MAX_VALUE, 75 ) );
		stringBufferPanelScrollPane.setPreferredSize( new Dimension( Integer.MAX_VALUE, 75 ) );
		stringBufferPanelScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_NEVER );
		stringBufferPanelScrollPane.getHorizontalScrollBar().setUnitIncrement( 16 );
		
		//Console
		consoleTextArea = new JTextArea( 3, 10 );
		consoleTextArea.setFont( font );
		consoleTextArea.setTabSize( 3 );
		consoleTextArea.setEditable( false );

		//Console scroll pane
		JScrollPane consoleScrollPane = new JScrollPane( consoleTextArea );

		//Panel for the console and registers
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout( new BoxLayout( bottomPanel, BoxLayout.Y_AXIS ) );
		bottomPanel.add( registersPanelScrollPane );
		bottomPanel.add( mainMemoryPanelScrollPane );
		bottomPanel.add( stringBufferPanelScrollPane );
		bottomPanel.add( consoleScrollPane );

		//Split pane to split code from the bottom pieces
		JSplitPane topSplitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
		topSplitPane.setTopComponent( codeScrollPane );
		topSplitPane.setBottomComponent( bottomPanel );
		topSplitPane.setDividerLocation( 300 );
		this.add( topSplitPane, BorderLayout.CENTER );

		//New button
		ImageIcon newIcon = getImageIcon( "new.png" );
		newButton = new JButton( newIcon );
		newButton.setFocusable( false );
		newButton.setToolTipText( "New file" );
		newButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				//Make sure they want to lose everything if there is something to lose
				if( fileHasChanged ) {
					int dialogResult = JOptionPane.showConfirmDialog(
							codeTextArea,
							"Creating a new file will destroy any changes in the current file.\n\nAre you sure you want to lose everything?",
							"Warning",
							JOptionPane.YES_NO_OPTION
							);

					//If they do
					if(dialogResult == JOptionPane.NO_OPTION){
						return;
					}

					//Close the current file
					closeFile();
				}
			}

		});

		//Open button
		ImageIcon openIcon = getImageIcon( "open.png" );
		openButton = new JButton( openIcon );
		openButton.setFocusable( false );
		openButton.setToolTipText( "Open file" );
		openButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				//Have them select a new file
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter( new FileNameExtensionFilter( "A100 Files", Config.fileExtension )  );
				fileChooser.setCurrentDirectory( currentFile ); 

				//Get their response
				int returnVal = fileChooser.showOpenDialog( openButton );

				//Open a file if they chose one
				if( returnVal == JFileChooser.APPROVE_OPTION ) {

					//Make sure they want to lose everything if there is something to lose
					if( fileHasChanged ) {

						//Make sure they want to lose everything
						int dialogResult = JOptionPane.showConfirmDialog(
								codeTextArea,
								"Opening a file will destroy any changes in the current file.\n\nAre you sure you want to lose everything?",
								"Warning",
								JOptionPane.YES_NO_OPTION
								);

						//If they do
						if(dialogResult == JOptionPane.NO_OPTION){
							return;
						}

					}

					//Close the current file
					closeFile();

					//Open the new file
					File file = fileChooser.getSelectedFile();
					openFile( file );
				}

			}

		});

		//Save button
		ImageIcon saveIcon = getImageIcon( "save.png" );
		saveButton = new JButton( saveIcon );
		saveButton.setFocusable( false );
		saveButton.setToolTipText( "Save file" );
		saveButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				doASave();
			}

		});

		//Step button
		ImageIcon stepIcon = getImageIcon( "step.png" );
		stepButton = new JButton( stepIcon );
		stepButton.setFocusable( false );
		stepButton.setEnabled( false );
		stepButton.setToolTipText( "Step forward" );
		stepButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				logic.step();
			}

		});


		//Fast forward button
		ImageIcon fastForwardIcon = getImageIcon( "fastforward.png" );
		fastForwardButton = new JButton( fastForwardIcon );
		fastForwardButton.setFocusable( false );
		fastForwardButton.setEnabled( false );
		fastForwardButton.setToolTipText( "Run to next pause" );
		fastForwardButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				fastForwardThread = new Thread( logic );
				fastForwardThread.start();

			}

		});

		//Run/Stop button
		runIcon = getImageIcon( "run.png" );
		stopIcon = getImageIcon( "stop.png" );
		runStopButton = new JButton( runIcon );
		runStopButton.setFocusable( false );
		runStopButton.setToolTipText( "Start running" );
		runStopButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				//If we're running, stop execution and switch to editing mode
				if( isRunning ) {

					//Stop the fast forward thread if it's running
					if( fastForwardThread != null ) {
						if( fastForwardThread.isAlive() ) {
							logic.halt = true;
						}
					}
					
					
					
					switchToEditMode();

				}else {

					//Otherwise, switch to execution mode
					switchToExecutionMode();

				}

			}

		});


		//Toolbar
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable( false );

		toolbar.add( newButton );
		toolbar.add( openButton );
		toolbar.add( saveButton );
		
		toolbar.addSeparator();

		toolbar.add( runStopButton );
		toolbar.add( stepButton );
		toolbar.add( fastForwardButton );

		this.add( toolbar, BorderLayout.NORTH );

		this.setVisible( true );

		//Make sure we properly set up the empty default file
		closeFile();

	}

	//Prints a string to the console
	public void print( String str ) {
		str = str.trim();
		
		consoleTextArea.append( str + "\n" );
	}

	//Prints an error to the console
	public void error( String str ) {

		//Print the error
		print( "Line #" + logic.lastLine + ": " + str );

		//Halt
		switchToEditMode();
	}

	//Clears out the console
	public void clearConsole() {
		consoleTextArea.setText( "" );
	}

	//Disable edit items items and enable execution items
	public void switchToEditMode() {
		
		//Enable editing items
		saveButton.setEnabled( true );
		newButton.setEnabled( true );
		openButton.setEnabled( true );
		codeTextArea.setEditable( true );
		codeTextArea.setFocusable( true );

		//Disable execution items
		stepButton.setEnabled( false );
		fastForwardButton.setEnabled( false );

		//Change the icon
		runStopButton.setIcon( runIcon );

		//Update the tooltip
		runStopButton.setToolTipText( "Start running" );

		//Display a selected line number while we're editing
		codeLineNumber.setCurrentLineForeground( Color.RED );
		codeLineNumber.repaint();

		//If there's a line highlighted, unhighlight it
		if( previousLineHighlighter != null ) {
			//Remove highlight from the previous line
			codeTextArea.getHighlighter().removeHighlight( previousLineHighlighter );
		}

		isRunning = false;

	}

	//Disable editing items and enable execution items
	public void switchToExecutionMode() {
		
		//Disable editing items
		saveButton.setEnabled( false );
		newButton.setEnabled( false );
		openButton.setEnabled( false );
		codeTextArea.setEditable( false );
		codeTextArea.setFocusable( false );

		//Enable execution items
		stepButton.setEnabled( true );
		fastForwardButton.setEnabled( true );

		//Change the icon
		runStopButton.setIcon( stopIcon );

		//Update the tooltip
		runStopButton.setToolTipText( "Stop running" );

		
		//Display a selected line number while we're editing
		codeLineNumber.setCurrentLineForeground( Color.BLACK );
		codeLineNumber.repaint();

		isRunning = true;
		
		//Clear the console
		clearConsole();
		
		//Reset the logic
		logic.getReadyToRun();
		
		//Preprocess the code
		logic.preprocess();

	}

	//Adds a register to the system
	public void addRegister( String name, int value ) {
		//The panel to contain the register
		JPanel register = new JPanel();
		register.setLayout( new BorderLayout() );
		register.setMinimumSize( new Dimension( 65, -1 ) );
		register.setPreferredSize( new Dimension( 65, -1 ) );

		//Add a border
		register.setBorder( BorderFactory.createLineBorder( Color.black ) );

		//The label to hold the register name
		JLabel topLabel = new JLabel( name );
		topLabel.setHorizontalAlignment( JLabel.CENTER );
		topLabel.setFont( font );
		register.add( topLabel, BorderLayout.NORTH );

		//The label to hold the register value
		JLabel bottomLabel = new JLabel( String.valueOf( value ) );
		bottomLabel.setHorizontalAlignment( JLabel.CENTER );
		bottomLabel.setFont( font );
		register.add( bottomLabel, BorderLayout.SOUTH );
		
		//Add the bottom register to the registerLabels map so it can be edited later
		registerLabels.put( name, bottomLabel );

		//Add this register to the list
		registersPanel.add( register );
		
		//Initialize this register in logic as well
		logic.addRegister( name, value );
	}
	
	//Adds a memory space to the UI
	public void addMainMemorySpace( int key, int value ) {
		//The panel to contain the memory space
		JPanel mainMemorySpace = new JPanel();
		mainMemorySpace.setLayout( new BorderLayout() );
		mainMemorySpace.setMinimumSize( new Dimension( 50, -1 ) );
		mainMemorySpace.setPreferredSize( new Dimension( 50, -1 ) );

		//Add a border
		mainMemorySpace.setBorder( BorderFactory.createLineBorder( Color.black ) );

		//The label to hold the memory space name
		JLabel topLabel = new JLabel( "M" + key );
		topLabel.setHorizontalAlignment( JLabel.CENTER );
		topLabel.setFont( font );
		mainMemorySpace.add( topLabel, BorderLayout.NORTH );

		//The label to hold the register value
		JLabel bottomLabel = new JLabel( String.valueOf( value ) );
		bottomLabel.setHorizontalAlignment( JLabel.CENTER );
		bottomLabel.setFont( font );
		mainMemorySpace.add( bottomLabel, BorderLayout.SOUTH );
		
		//Add the bottom label to the mainMemoryLabels array so it can be edited later
		mainMemoryLabels[ key ] = bottomLabel;

		//Add this memory space to the list
		mainMemoryPanel.add( mainMemorySpace );
	}
	
	public void addStringBufferCharacter( int key, String value ) {
		
		//The panel to contain the string buffer character
		JPanel stringBufferCharacter = new JPanel();
		stringBufferCharacter.setLayout( new BorderLayout() );
		stringBufferCharacter.setMinimumSize( new Dimension( 50, -1 ) );
		stringBufferCharacter.setPreferredSize( new Dimension( 50, -1 ) );

		//Add a border
		stringBufferCharacter.setBorder( BorderFactory.createLineBorder( Color.black ) );

		//The label to hold the character index
		JLabel topLabel = new JLabel( String.valueOf( key ) );
		topLabel.setHorizontalAlignment( JLabel.CENTER );
		topLabel.setFont( font );
		stringBufferCharacter.add( topLabel, BorderLayout.NORTH );

		//The label to hold the character string
		JLabel bottomLabel = new JLabel( String.valueOf( value ) );
		bottomLabel.setHorizontalAlignment( JLabel.CENTER );
		bottomLabel.setFont( font );
		stringBufferCharacter.add( bottomLabel, BorderLayout.SOUTH );
		
		//Add the bottom label to the string buffer array so it can be edited later
		stringBufferLabels[ key ] = bottomLabel;

		//Add this memory space to the list
		stringBufferPanel.add( stringBufferCharacter );
	}
	
	//This highlights a memory address as being the currently selected one
	public void highlightMemoryAddress( int address ) {
		
		//Unhighlight the previous memory address if there is one
		if( previousMemoryAddress != null ) {
			( (JPanel) previousMemoryAddress.getParent() ).setBorder( BorderFactory.createLineBorder( Color.black ) );
		}	
		
		//Highlight the new address
		( (JPanel) mainMemoryLabels[ address ].getParent() ).setBorder( BorderFactory.createLineBorder( Config.highlightedMemoryAddressColor, 3 ) );
		
		//Save the new address as the previous memory address
		previousMemoryAddress = mainMemoryLabels[ address ];
		
		//Scroll to the newly highlighted address
		JScrollBar bar = mainMemoryPanelScrollPane.getHorizontalScrollBar();
		
		//Calculate our address percentage
		float addressPercent = ( (float) address / (float) Config.mainMemoryLength );
		
		//Calculate the bar value to match our address percentage
		float barValue = addressPercent * (float) bar.getMaximum();
		
		//Set our bar value to that
		bar.setValue( (int) barValue );
		
		
	}
	
	//Change the text of a register label
	public void setRegisterValue( String name, int value ) {
		registerLabels.get( name ).setText( String.valueOf( value ) );
	}
	
	//Change the text of a memory label
	public void setMainMemoryValue( int address, int value ) {
		mainMemoryLabels[ address ].setText( String.valueOf( value ) );
	}
	
	//Change the text of a string buffer label
	public void setStringBufferValue( int address, String value ) {
		stringBufferLabels[ address ].setText( value );
	}
	
	//Returns whether or not a file is open
	public boolean isFileOpen() {
		return !openFile.equals( "" );
	}

	//Figures out what kind of save to do and does it
	public void doASave() {

		//If the file has changed, show the save dialog
		//If there is already a file open, just save to that
		if( isFileOpen() ) {
			saveFile( currentFile );
		}else {

			//Otherwise, show the dialog
			showSaveFileDialog();
		}

	}

	//Shows the save file dialog
	public void showSaveFileDialog() {

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter( new FileNameExtensionFilter( "A100 Files", Config.fileExtension )  );
		fileChooser.setCurrentDirectory( currentFile ); 

		//Get their response
		int returnVal = fileChooser.showSaveDialog( codeTextArea );

		//Open a file if they chose one
		if( returnVal == JFileChooser.APPROVE_OPTION ) {

			File saveFile = fileChooser.getSelectedFile();

			//If it doesn't end in the appropriate extension, add it on
			if( !saveFile.getName().toLowerCase().endsWith( "." + Config.fileExtension.toLowerCase() ) ) {
				saveFile = new File( saveFile.getAbsolutePath() + "." + Config.fileExtension );
			}

			saveFile( saveFile );

		}

	}

	//Actually saves the currently open file
	public void saveFile( File file ) {

		//Writer to save the file
		try {
			FileWriter writer = new FileWriter( file );

			//Save the file
			writer.write( codeTextArea.getText() );

			//Close the writer
			writer.flush();
			writer.close();

			//If we just saved, make sure we know we don't have changes left
			setFileHasChanged( false );

			//Update our recent file path
			currentFile = file;

			//Keep track of our file name
			openFile = file.getName();

			//Update the title
			updateTitle();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//Opens a file
	public void openFile( File file ) {

		//Update the open file
		openFile = file.getName();

		//Mark down that this is the path to the most recent file
		currentFile = file;

		//The contents of the file
		String fileContents = "";

		try {

			//Scanner to read the file
			Scanner scanner = new Scanner( new FileReader( file ) );

			//Read the contents
			while( scanner.hasNext() ) {

				String line = scanner.nextLine();

				fileContents += line + System.lineSeparator();

			}

			//Close the scanner
			scanner.close();

			//Put the loaded file in the text area
			codeTextArea.setText( fileContents );

			//Update the title
			updateTitle();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Just opened files have no changes
		setFileHasChanged( false );

	}

	//Closes the current file
	public void closeFile() {

		//We have no file open
		openFile = "";

		//Update the title to reflect that
		updateTitle();

		//Empty any text that might linger
		codeTextArea.setText( "" );

		//Empty files have no changes
		setFileHasChanged( false );
	}
	
	//Returns an icon image for a given file name
	public ImageIcon getImageIcon( String fileName ) {
		return new ImageIcon( Config.iconPath + fileName );
	}

	//Updates the title of the window
	public void updateTitle() {
		if( isFileOpen() ) {
			this.setTitle( Config.titleBase + " - " + openFile );
		}else {
			this.setTitle( Config.titleBase + " - Untitled" );
		}
	}

	//Updates the file having changed
	public void setFileHasChanged( boolean newValue ) {
		fileHasChanged = newValue;
	}
	
	//Gets and returns the line count
	public int getLineCount() {
		return codeTextArea.getLineCount();
	}
	
	//Highlights a given line
	public void highlightLine( int lineNumber ) {
		
		//Only if we have a previous line
		if( previousLineHighlighter != null ) {
			//Remove highlight from the previous line
			codeTextArea.getHighlighter().removeHighlight( previousLineHighlighter );
		}

		try {
			int lineStart = codeTextArea.getLineStartOffset( lineNumber );
			int lineEnd = codeTextArea.getLineEndOffset( lineNumber );
			previousLineHighlighter = codeTextArea.getHighlighter().addHighlight( lineStart, lineEnd, new DefaultHighlighter.DefaultHighlightPainter( Config.highlightedColor ) );
		} catch (BadLocationException e) {
			
			//If this line doesn't exist, don't even try to highlight it as we have other methods to deal with these errors internally
			//And I hate stacks printing in console needlessly
			
		}
		
	}
	
	//Returns the text from a given line
	public String getLine( int lineNumber ) {
		
		String line = "";
		
		try {
			int lineStart = codeTextArea.getLineStartOffset( lineNumber );
			int lineEnd = codeTextArea.getLineEndOffset( lineNumber );
			line = codeTextArea.getText( lineStart ,  lineEnd - lineStart );
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return line;
		
	}

}
