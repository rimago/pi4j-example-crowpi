package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.SevenSegmentComponent;
import com.pi4j.context.Context;

public class SevenSegmentApp implements Application {
    @Override
    public void execute(Context pi4j) {
        final var segment = new SevenSegmentComponent(pi4j);
        segment.setEnabled(true);
        segment.setBlinkRate(0);
        segment.setBrightness(15);

        segment.setColon(true);
        segment.setDigit(0, 4);
        segment.setDigit(1, 2);
        segment.setCharacter(2, 'A');
        segment.setCharacter(3, 'B');
        segment.refresh();
    }
}
