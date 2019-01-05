package misc;
import java.awt.Color;

public class Config {

	/*
	 * Application Settings
	 */
	
	//The program title
	public static String titleBase = "A100";
	
	//The file extension to expect
	public static String fileExtension = "A1";
	
	//Highlighted line color
	public static Color highlightedColor = Color.GRAY;
	
	//Highlighted memory address color
	public static Color highlightedMemoryAddressColor = Color.RED;
	
	//The font size
	public static int fontSize = 18;
	
	//Path to the icons
	public static String iconPath = "resources/icons/";
	
	/*
	 * Compiler Settings
	 * 
	 */
	
	//The range of numbers the system can use
	public static int minNumberRange = -9999;
	public static int maxNumberRange = 9999;
	
	//The number of non-special registers
	public static int registerCount = 7;
	
	//The size of main memory
	public static int mainMemoryLength = 10000;

}
