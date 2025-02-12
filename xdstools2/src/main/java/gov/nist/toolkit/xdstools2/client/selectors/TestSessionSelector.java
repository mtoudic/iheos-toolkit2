package gov.nist.toolkit.xdstools2.client.selectors;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.xdstools2.client.PasswordManagement;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionChangedEvent;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionChangedEventHandler;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionsUpdatedEvent;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionsUpdatedEventHandler;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.client.util.TabWatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class TestSessionSelector {
    ListBox listBox = new ListBox();
    TextBox textBox = new TextBox();
    HorizontalPanel panel;
    static final String NONSELECTION = "--Select--";

    public TestSessionSelector(List<String> initialContents, String initialSelection, TabWatcher tabWatcher) {
//        Xdstools2.DEBUG("initialize TestSessionSelector with " + initialContents + " ==>" + initialSelection);
        build(initialContents, initialSelection);
        link();
    }

    public TestSessionSelector(List<String> initialContents, String initialSelection) {
//        Xdstools2.DEBUG("initialize TestSessionSelector with " + initialContents + " ==>" + initialSelection);
        this(initialContents, initialSelection, null);
    }

    // Listen on the EventBus in the future
    private void link() {

        // Test sessions reloaded
        ClientUtils.INSTANCE.getEventBus().addHandler(TestSessionsUpdatedEvent.TYPE, new TestSessionsUpdatedEventHandler() {
            @Override
            public void onTestSessionsUpdated(TestSessionsUpdatedEvent event) {
                listBox.clear();
                for (String i : event.testSessionNames) listBox.addItem(i);
            }
        });


        // SELECT
        ClientUtils.INSTANCE.getEventBus().addHandler(TestSessionChangedEvent.TYPE, new TestSessionChangedEventHandler() {
            @Override
            public void onTestSessionChanged(TestSessionChangedEvent event) {
                if (event.getChangeType() == TestSessionChangedEvent.ChangeType.SELECT) {
                    listBox.setSelectedIndex(indexOfValue(event.getValue()));
                }
            }
        });
    }

    // Initialize screen now
    private void build(List<String> initialContents, String initialSelection) {
        if (initialContents == null) initialContents = new ArrayList<>();
        List<String> contents = new ArrayList<>();

        contents.add(NONSELECTION);
        contents.addAll(initialContents);


        panel = new HorizontalPanel();

        panel.add(new HTML("Test Session: "));

        //
        // List Box
        //
        listBox.removeStyleName("testSessionSelectorMc");
        listBox.addStyleDependentName("testSessionSelectorMc");

        for (String i : contents) listBox.addItem(i);
        if (contents.contains(initialSelection))
            listBox.setSelectedIndex(contents.indexOf(initialSelection));
        else
            listBox.setSelectedIndex(0);
        panel.add(listBox);
        listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                String newValue = listBox.getValue(listBox.getSelectedIndex());
                if (NONSELECTION.equals(newValue)) return;
                doChange(newValue);
            }
        });

        textBox.removeStyleName("testSessionInputMc");
        textBox.addStyleName("testSessionInputMc");
        panel.add(textBox);

        textBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                boolean enterPressed = KeyCodes.KEY_ENTER == keyUpEvent.getNativeEvent().getKeyCode();
                if (enterPressed) {
                    add();
                }
            }
        });

        //
        // Add Button
        //
        Button addTestSessionButton = new Button("Add");
        panel.add(addTestSessionButton);
        addTestSessionButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                String value = textBox.getValue().trim();
                textBox.setValue("");
                if ("".equals(value)) return;
                ClientUtils.INSTANCE.getEventBus().fireEvent(new TestSessionChangedEvent(TestSessionChangedEvent.ChangeType.ADD, value));
            }
        });

        //
        // Delete Button
        //
        Button delTestSessionButton = new Button("Delete");
        panel.add(delTestSessionButton);
        delTestSessionButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                String value = listBox.getValue(listBox.getSelectedIndex());
                ClientUtils.INSTANCE.getEventBus().fireEvent(new TestSessionChangedEvent(TestSessionChangedEvent.ChangeType.DELETE, value));
            }
        });
    }

    void doChange(String newValue) {
        ClientUtils.INSTANCE.getTestSessionManager().setCurrentTestSession(newValue);
        ClientUtils.INSTANCE.getEventBus().fireEvent(new TestSessionChangedEvent(TestSessionChangedEvent.ChangeType.SELECT, newValue));
    }

    void add() {
        String value = textBox.getValue().trim();
        value = value.replaceAll(" ", "_");
        textBox.setValue("");
        if ("".equals(value)) return;
        ClientUtils.INSTANCE.getEventBus().fireEvent(new TestSessionChangedEvent(TestSessionChangedEvent.ChangeType.ADD, value));
    }

    public Widget asWidget() { return panel; }

    int indexOfValue(String value) {
        for (int i=0; i<listBox.getItemCount(); i++)
            if (listBox.getItemText(i).equals(value))
                return i;
        return -1;
    }

    public void setVisibility(boolean visible) {
        panel.setVisible(visible);
    }
}
