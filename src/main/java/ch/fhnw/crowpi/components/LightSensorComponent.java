package ch.fhnw.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

/**
 * Implementation of the CrowPi light sensor using I2C with Pi4J
 */
public class LightSensorComponent {
    private final I2C i2c;

    /**
     * Those default address are to use this class with default CrowPi setup
     */
    private static final int DEFAULT_BUS = 0x1;
    private static final int DEFAULT_DEVICE = 0x5c;

    // Start measurement at 1lx resolution. Time typically 120ms
    private static final int ONE_TIME_HIGH_RES_MODE_1 = 0x20;
    // Start measurement at 0.5lx resolution. Time typically 120ms
    private static final int ONE_TIME_HIGH_RES_MODE_2 = 0x21;
    // Start measurement at 1lx resolution. Time typically 120ms
    private static final int ONE_TIME_LOW_RES_MODE = 0x23;

    /**
     * Creates a new light sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public LightSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    /**
     * Creates a new buzzer component with a custom BCM pin.
     *
     * @param pi4j   Pi4J context
     * @param bus    Custom I2C bus address
     * @param device Custom device address on I2C
     */
    public LightSensorComponent(Context pi4j, int bus, int device) {
        this.i2c = pi4j.create(buildI2CConfig(pi4j, bus, device));
    }

    /**
     * Read current light strength in lux with custom resolution settings
     *
     * @param resolution Defines resolution of measurements: 0 = 4lx Resolution / 1 = 1lx Resolution / 2 = 0.5lx Resolution
     * @return current light in lux
     */
    public double readLight(int resolution) {
        if (resolution > 2 || resolution < 0) {
            throw new IllegalArgumentException("Invalid Resolution Selected");
        }

        int resolutionRegisterValue = 0;
        switch (resolution) {
            case 0:
                resolutionRegisterValue = ONE_TIME_LOW_RES_MODE;
                break;
            case 1:
                resolutionRegisterValue = ONE_TIME_HIGH_RES_MODE_1;
                break;
            case 2:
                resolutionRegisterValue = ONE_TIME_HIGH_RES_MODE_2;
                break;
        }

        // Measurement divided by 1.2 gives a value in lux. According to BH1750 manual.
        return i2c.readRegisterWord(resolutionRegisterValue) / 1.2;
    }

    /**
     * Measure current light with default resolution (1lx)
     *
     * @return current light in lux
     */
    public double readLight() {
        // Measurement divided by 1.2 gives a value in lux. According to BH1750 manual.
        return i2c.readRegisterWord(ONE_TIME_HIGH_RES_MODE_1) / 1.2;
    }

    /**
     * @param pi4j   PI4J Context
     * @param bus    I2C Bus address
     * @param device I2C Device address
     * @return I2C configuration
     */
    private static I2CConfig buildI2CConfig(Context pi4j, int bus, int device) {
        return I2C.newConfigBuilder(pi4j)
                .id("I2C-" + device + "@" + bus)
                .name("Light Sensor")
                .bus(bus)
                .device(device)
                .build();
    }
}