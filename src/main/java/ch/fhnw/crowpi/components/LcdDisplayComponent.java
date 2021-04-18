package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.internal.MCP23008;
import com.pi4j.context.Context;

// Diese Ding benutzt IO via MCP23008.
// Also I2C -> MCP23008 -> IO -> Display
// Brauch MCP23008 die richtig auf IO schreibt
// braucht komponente die richtig auf MCP schreibt
// helligkeit nur mit poti
public class LcdDisplayComponent {
    /**
     * IO Component used to Display
     */
    private final MCP23008 mcp;
    /**
     * Those default address are to use this class with default CrowPi setup
     */
    private static final int DEFAULT_BUS = 0x1;
    private static final int DEFAULT_DEVICE = 0x21;

    /**
     * Creates a new LCD Display component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public LcdDisplayComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    /**
     * Creates a new  LCD Display component with custom bus, device address
     *
     * @param pi4j   Pi4J context
     * @param bus    Custom I2C bus address
     * @param device Custom device address on I2C
     */
    public LcdDisplayComponent(Context pi4j, int bus, int device) {
        this.mcp = new MCP23008(pi4j, bus, device);
        this.mcp.initializeIo(MCP_IO_CONFIG);
    }

    // FIXME IM JUST A TEST METHOD!
    public void play() {
        write('A', true);
    }

    /**
     * Initializes the LCD Display
     */
    public void initialize() {
        // Enable backlight
        setDisplayBacklight(true);

        // Initialize display
        write((byte) 0b001_10011);
        write((byte) 0b001_10010);

        // Initialize display settings
        byte displayControl = (byte) (LCD_DISPLAYON | LCD_CURSOROFF | LCD_BLINKOFF);
        byte displayFunction = (byte) (LCD_4BITMODE | LCD_1LINE | LCD_2LINE | LCD_5x8DOTS);
        byte displayMode = (byte) (LCD_ENTRYLEFT | LCD_ENTRYSHIFTDECREMENT);

        // Write Display settings
        executeCommand(LCD_DISPLAYCONTROL, displayControl);
        write((byte) (LCD_FUNCTIONSET | displayFunction));
        write((byte) (LCD_ENTRYMODESET | displayMode));

        // Clear display
        clearDisplay();
    }

    /**
     * Write a Line of Text on the LCD Display
     *
     * @param text Text to display
     * @param line Select Line of Display
     */
    public void writeLine(String text, int line) {
        returnHome();
        clearLine(line);
        setCursorToLine(line);

        for (int i = 0; i < text.length(); i++) {
            write(text.charAt(i), true);
        }
    }

    /**
     * Write a text upto 32 characters to the display
     *
     * @param text Text to write to the display
     */
    public void writeText(String text) {
        setCursorToLine(1);

        for (int i = 0; i < text.length(); i++) {
            // line break in text found
            if (text.charAt(i) == '\n') {
                setCursorToLine(2);
                continue;
            }

            // write character to display
            write(text.charAt(i), true);

            // was last character on first line? switch to second
            if (i == 15) {
                setCursorToLine(2);
            }
        }
    }

    public void setCursorToLine(int number) {
        if (number > 2 || number < 1) {
            throw new IllegalArgumentException("CrowPi Display has only 2 Rows!");
        }

        executeCommand(LCD_SETDDRAMADDR, LCD_ROW_OFFSETS[number - 1]);
    }

    public void createOwnCharacter(int number, byte[] character) {
        if (character.length > 7) {
            throw new IllegalArgumentException("Array to long. Character is only 5x8 Digits. Only a array with length" +
                " 8 is allowed");
        }

        number &= 0x7;
        write(LCD_SETCGRAMADDR | (number << 3));

        for (int i = 0; i < 8; i++) {
            write(character[i], true);
        }

    }


    /**
     * Enable and Disable the Backlight of the LCD Display
     *
     * @param state Set Backlight ON or OFF
     */
    public void setDisplayBacklight(boolean state) {
        mcp.setAndWritePin(LCD_LIGHT, state);
    }

    /**
     * Clears the display and return the cursor to home
     */
    public void clearDisplay() {
        write(LCD_CLEARDISPLAY);
        sleep(3);
        returnHome();
    }

    /**
     * Clears a line of the display
     *
     * @param line Select line to clear
     */
    public void clearLine(int line) {
        setCursorToLine(line);

        for (int i = 0; i < 16; i++) {
            write(' ', true);
        }
    }

    /**
     * Returns the Cursor to Home Position (First line, first character)
     */
    public void returnHome() {
        write(LCD_RETURNHOME);
        sleep(3);
    }

    /**
     * Execute Display commands
     *
     * @param command Select the LCD Command
     * @param data    Setup command data
     */
    protected void executeCommand(byte command, byte data) {
        write((byte) (command | data));
    }

    /**
     * Write a number (byte) to the LCD Display
     *
     * @param c Number to write to the Display
     */
    protected void write(int c) {
        write(c, false);
    }

    /**
     * Write a Number (byte) or character according to the LCD Display
     *
     * @param b        Data to write to the display
     * @param charMode Select data is a number or character
     */
    protected void write(int b, boolean charMode) {
        b &= 0xFF;
        mcp.setAndWritePin(LCD_RS, charMode);

        // high nibble
        mcp.setPin(LCD_D4, (b & 0b0001_0000) > 0);
        mcp.setPin(LCD_D5, (b & 0b0010_0000) > 0);
        mcp.setPin(LCD_D6, (b & 0b0100_0000) > 0);
        mcp.setPin(LCD_D7, (b & 0b1000_0000) > 0);
        mcp.writePins();
        mcp.pulsePin(LCD_EN, DEFAULT_PULSE_WIDTH);

        // low nibble
        mcp.setPin(LCD_D4, (b & 0b0000_0001) > 0);
        mcp.setPin(LCD_D5, (b & 0b0000_0010) > 0);
        mcp.setPin(LCD_D6, (b & 0b0000_0100) > 0);
        mcp.setPin(LCD_D7, (b & 0b0000_1000) > 0);
        mcp.writePins();
        mcp.pulsePin(LCD_EN, DEFAULT_PULSE_WIDTH);
    }


    /**
     * FIXME with component sleep!
     *
     * @param millis
     */
    public void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Default MCP Configuration
    byte MCP_IO_CONFIG = 0x00;

    // Default Pulsewidth
    private final int DEFAULT_PULSE_WIDTH = 1;

    // Commands
    byte LCD_CLEARDISPLAY = 0x01;
    byte LCD_RETURNHOME = 0x02;
    byte LCD_ENTRYMODESET = 0x04;
    byte LCD_DISPLAYCONTROL = 0x08;
    byte LCD_CURSORSHIFT = 0x10;
    byte LCD_FUNCTIONSET = 0x20;
    byte LCD_SETCGRAMADDR = 0x40;
    byte LCD_SETDDRAMADDR = (byte) 0x80;


    // Entry flags
    byte LCD_ENTRYRIGHT = 0x00;
    byte LCD_ENTRYLEFT = 0x02;
    byte LCD_ENTRYSHIFTINCREMENT = 0x01;
    byte LCD_ENTRYSHIFTDECREMENT = 0x00;


    // Control flags
    byte LCD_DISPLAYON = 0x04;
    byte LCD_DISPLAYOFF = 0x00;
    byte LCD_CURSORON = 0x02;
    byte LCD_CURSOROFF = 0x00;
    byte LCD_BLINKON = 0x01;
    byte LCD_BLINKOFF = 0x00;

    // Move flags
    byte LCD_DISPLAYMOVE = 0x08;
    byte LCD_CURSORMOVE = 0x00;
    byte LCD_MOVERIGHT = 0x04;
    byte LCD_MOVELEFT = 0x00;

    // Function set flags
    byte LCD_8BITMODE = 0x10;
    byte LCD_4BITMODE = 0x00;
    byte LCD_2LINE = 0x08;
    byte LCD_1LINE = 0x00;
    byte LCD_5x10DOTS = 0x04;
    byte LCD_5x8DOTS = 0x00;

    // Offset forup to 4 rows.
    byte[] LCD_ROW_OFFSETS = {0x00, 0x40, 0x14, 0x54};

    // Char LCDplate buttonnames.
    int SELECT = 0;
    int RIGHT = 1;
    int DOWN = 2;
    int UP = 3;
    int LEFT = 4;

    /**
     * Pin out LCD auf MCP
     */
    private final int LCD_RS = 1;
    private final int LCD_EN = 2;
    private final int LCD_D4 = 3;
    private final int LCD_D5 = 4;
    private final int LCD_D6 = 5;
    private final int LCD_D7 = 6;
    private final int LCD_LIGHT = 7;
}