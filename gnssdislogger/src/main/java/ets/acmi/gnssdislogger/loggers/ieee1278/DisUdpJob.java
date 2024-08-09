package ets.acmi.gnssdislogger.loggers.ieee1278;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import org.slf4j.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.common.events.UploadEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;

class DisUdpJob extends Job {

    private static final Logger LOG = Logs.of(DisUdpJob.class);
    private final String server;
    private final int port;
    private final byte[] buffer;

    // Class constructor
    public DisUdpJob(String server, int port, byte[] buffer) {
        super(new Params(1).requireNetwork().persist());
        this.server = server;
        this.port = port;
        this.buffer = buffer;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        LOG.info("Logging to IEEE1278 Job Running");
        sendEntityStatePDU();
        EventBus.getDefault().post(new UploadEvents.IEEE1278().succeeded());
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        LOG.error("Logging to IEEE1278 Job Running", throwable);
        EventBus.getDefault().post(new UploadEvents.IEEE1278().failed("Could not send to IEEE1278 Server", throwable));
        return RetryConstraint.CANCEL;
    }


    private void sendEntityStatePDU() throws Exception {
        DatagramSocket udpSocket = new DatagramSocket(port);
        InetAddress serverAddr = InetAddress.getByName(server);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddr, port);
        udpSocket.send(packet);
        udpSocket.close();
        LOG.info("IEEE1278 packet sent to " + server + ":" + port);
    }
}
