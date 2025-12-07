package it.unibo.oop.reactivegui03;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unibo.oop.JFrameUtil;

/**
 * Third experiment with reactive gui.
 */
public final class AnotherConcurrentGUI extends JFrame {
    @Serial
    private static final long MILLIES = 10_000;
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AnotherConcurrentGUI.class);
    private final JLabel display = new JLabel();
    private final Agent agent = new Agent();
    private final JButton up = new JButton("up");
    private final JButton down = new JButton("down");
    private final JButton stop = new JButton("stop");

    /**
     * Builds a new CGUI.
     */
    public AnotherConcurrentGUI() {
        super();
        JFrameUtil.dimensionJFrame(this);
        final JPanel panel = new JPanel();
        panel.add(display);
        panel.add(up);
        panel.add(down);
        panel.add(stop);
        this.getContentPane().add(panel);
        this.setVisible(true);
        /*
         * Create the counter agent and start it. This is actually not so good:
         * thread management should be left to
         * java.util.concurrent.ExecutorService
         */

        final AgentSleep agentSleep = new AgentSleep();
        new Thread(agent).start();
        //final ExecutorService executor = Executors.newFixedThreadPool(2);
        //executor.execute(agent);
        //executor.execute(agentSleep);
        new Thread(agentSleep).start();
        /*
         * Register a listener that stops it
         */
        stop.addActionListener(e -> {
            agent.stopCounting();
            down.setEnabled(false);
            up.setEnabled(false);
            stop.setEnabled(false);
        });
        down.addActionListener(e -> agent.decrement());
        up.addActionListener(e -> agent.increment());
    }

    /*
     * The counter agent is implemented as a nested class. This makes it
     * invisible outside and encapsulated.
     */
    private final class Agent implements Runnable, Serializable {
        /*
         * Stop is volatile to ensure visibility. Look at:
         *
         * http://archive.is/9PU5N - Sections 17.3 and 17.4
         *
         * For more details on how to use volatile:
         *
         * http://archive.is/4lsKW
         *
         */
        private static final long serialVersionUID = 1L;

        private volatile boolean decrement;
        private volatile boolean stop;
        private int counter;

        @Override
        public void run() {
            while (!this.stop) {
                if (!this.decrement) {
                    try {
                        // The EDT doesn't access `counter` anymore, it doesn't need to be volatile
                        final var nextText = Integer.toString(this.counter);
                        SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(nextText));
                        this.counter++;
                        Thread.sleep(100);
                    } catch (InvocationTargetException | InterruptedException ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                } else {
                    try {
                        // The EDT doesn't access `counter` anymore, it doesn't need to be volatile
                        final var nextText = Integer.toString(this.counter);
                        SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(nextText));
                        this.counter--;
                        Thread.sleep(100);
                    } catch (InvocationTargetException | InterruptedException ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }
            }
        }

        /**
         * External command to stop counting.
         */
        public void stopCounting() {
            this.stop = true;
        }

        public void decrement() {
            this.decrement = true;
        }

        public void increment() {
            this.decrement = false;
        }
    }

    private final class AgentSleep implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(MILLIES);
                agent.stopCounting();
                down.setEnabled(false);
                up.setEnabled(false);
                stop.setEnabled(false);
            } catch (final InterruptedException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        } 
    }
}
