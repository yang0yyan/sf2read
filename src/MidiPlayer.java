import java.util.List;
import java.util.concurrent.*;

public class MidiPlayer {
    ExecutorService executorService = new ThreadPoolExecutor(3, 5, 1L, TimeUnit.SECONDS, new ArrayBlockingQueue(4), Executors.defaultThreadFactory());
    List<MidiRead.Chunk> list;

    public void setData(List<MidiRead.Chunk> list) {
        this.list = list;
    }

    public void play() {
        for (int i = 0; i < list.size(); i++) {
            MidiRead.Chunk chunk = list.get(i);
            if (chunk.id.equals("MThd")) {
                System.out.println(chunk.toString());
            } else if (chunk.id.equals("MTrk")) {
                executorService.execute(new EventThread(chunk.mTrkEventArrayList, i));
            }
        }
        executorService.shutdown();
    }

    class EventThread implements Runnable {
        List<MidiRead.MTrkEvent> list;
        int currentIndex = 0;
        int tag = 0;

        public EventThread(List<MidiRead.MTrkEvent> list, int tag) {
            this.list = list;
            this.tag = tag;
        }

        @Override
        public void run() {
            MidiRead.MTrkEvent mTrkEvent = list.get(currentIndex);
            try {
                Thread.sleep(mTrkEvent.deltaTime * 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (tag == 2) {
                System.out.println(mTrkEvent.toString());
            }
            currentIndex++;
            if (currentIndex < list.size()) {
                run();
            }
        }
    }
}
