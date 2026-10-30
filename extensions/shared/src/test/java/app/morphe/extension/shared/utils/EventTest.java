package app.morphe.extension.shared.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/** Ensures callback registration changes take effect on the next notification. */
public class EventTest {
    @Test
    public void observerCanRemoveItselfWithoutSkippingOtherObservers() {
        Event<Integer> event = new Event<>();
        List<Integer> received = new ArrayList<>();
        event.addObserver(new Function1<>() {
            @Override
            public Unit invoke(Integer value) {
                received.add(value);
                event.removeObserver(this);
                return Unit.INSTANCE;
            }
        });
        event.addObserver(value -> {
            received.add(value * 10);
            return Unit.INSTANCE;
        });

        event.invoke(1);
        event.invoke(2);

        assertEquals(List.of(1, 10, 20), received);
    }

    @Test
    public void observerAddedDuringNotificationStartsWithTheNextNotification() {
        Event<Integer> event = new Event<>();
        List<Integer> received = new ArrayList<>();
        Function1<Integer, Unit> added = value -> {
            received.add(value);
            return Unit.INSTANCE;
        };
        event.addObserver(value -> {
            event.addObserver(added);
            return Unit.INSTANCE;
        });

        event.invoke(1);
        assertEquals(List.of(), received);
        event.invoke(2);
        assertEquals(List.of(2), received);
    }
}
