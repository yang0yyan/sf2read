import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class MidiRead {

    private long fileIndex = 0;

    private byte[] b1 = new byte[1];
    private byte[] b2 = new byte[2];
    private byte[] b4 = new byte[4];

    private RandomAccessFile mRandomAccessFile;
    private long fileLength;
    private long absoluteTime = 0;

    private List<Chunk> chunkList = new ArrayList<>();

    public void getFileStream(String filePath, String fileName) {
        File file = new File(filePath, fileName);

        try {
            mRandomAccessFile = new RandomAccessFile(file, "r");
            readStream();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Chunk> getChunkList() {
        return chunkList;
    }

    private void readStream() throws IOException {
        fileLength = mRandomAccessFile.length();
        System.out.println(fileLength);
        readMIDI();
//        System.out.println(chunkList.toString());
    }

    int index = 0;

    private void readMIDI() throws IOException {

        Chunk chunk = readChunk();
        chunkList.add(chunk);
        if (chunk.id.equals("MThd")) {
            chunk.describe = "文件头块";
            readMThd(chunk);
        } else if (chunk.id.equals("MTrk")) {
            chunk.describe = "音轨块";
            absoluteTime = 0;
            readMTrk(chunk);
        }

//        if (chunk.id.equals("MTrk")) {
//            absoluteTime = 0;
//            readMTrk(chunk);
//            index++;
//        }

        if (fileLength > chunk.size + chunk.index) {
            seek(chunk.size + chunk.index);
            readMIDI();
        } else {
            if (fileLength == chunk.size + chunk.index) {
                System.out.println("大小相符");
            } else {
                System.out.println("大小不相符");
            }
            System.out.println("读完");
        }
    }

    private void readMThd(Chunk chunk) throws IOException {
        if (chunk.size == 6) {
            Map<String, String> map = new HashMap<>();
            int format = readUnsignedShort();
            int ntrks = readUnsignedShort();
            int division = readUnsignedShort();
            if (format == 0) {
                map.put("format", format + "单音轨");
            } else if (format == 1) {
                map.put("format", format + "多音轨，且同步");
            } else if (format == 2) {
                map.put("format", format + "多音轨，但不同步");
            }
            map.put("ntrks", "音轨块数量：" + ntrks);
            map.put("division", "DTT：" + division);
            chunk.data = map.toString();
        }
    }

    private void readMTrk(Chunk chunk) throws IOException {
        MTrkEvent mTrkEvent = readMTrkEvent();
        if (mTrkEvent.event == null) return;
        Event event = mTrkEvent.event;
        chunk.mTrkEventArrayList.add(mTrkEvent);
        if (chunk.size > event.size + event.index - chunk.index) {
            seek(event.size + event.index);
            readMTrk(chunk);
        } else {
            if (chunk.size == event.size + event.index - chunk.index) {
                System.out.println("大小相符");
            } else {
                System.out.println("大小不相符");
            }
            System.out.println("读完");
        }
    }

    private int readLen() throws IOException {
        fileIndex += 1;
        return mRandomAccessFile.read(b1);
    }

    private int read2Len() throws IOException {
        fileIndex += 2;
        return mRandomAccessFile.read(b2);
    }

    private int read4Len() throws IOException {
        fileIndex += 4;
        return mRandomAccessFile.read(b4);
    }

    // Read 8 bit unsigned integer from stream
    public int readUnsignedByte() throws IOException {
        readLen();
        return (b1[0] & 0xFF);
    }

    // Read 16 bit unsigned integer from stream
    public int readUnsignedShort() throws IOException {
        read2Len();
        return ((b2[0] & 0xFF) << 8) | (b2[1] & 0xFF);
    }

    // Read 32 bit unsigned integer from stream
    public long readUnsignedInt() throws IOException {
        read4Len();
        return ((b4[0] & 0xFF) << 24) | ((b4[1] & 0xFF) << 16) | ((b4[2] & 0xFF) << 8) | (b4[3] & 0xFF);
    }

    public String readString(int len) throws IOException {
        byte[] bytes = new byte[len];
        fileIndex += len;
        mRandomAccessFile.read(bytes);
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                bytes[i] = 0X20;
            }
        }
        return new String(bytes, "ascii");
    }

    private Chunk readChunk() throws IOException {
        Chunk chunk = new Chunk();
        chunk.id = readString(4);
        chunk.size = readUnsignedInt();
        chunk.index = fileIndex;
        return chunk;
    }


    private Chunk readEvent() throws IOException {
        Chunk chunk = new Chunk();
        int type = readUnsignedShort();
        if (type == 255) {
            chunk.id = Integer.toHexString(readUnsignedByte());
            chunk.size = readUnsignedByte();
            chunk.index = fileIndex;
            return chunk;
        }
        return null;
    }


    private MTrkEvent readMTrkEvent() throws IOException {
        MTrkEvent mTrkEvent = new MTrkEvent();
        mTrkEvent.deltaTime = readDeltaTime();
//        absoluteTime +=mTrkEvent.deltaTime;
        mTrkEvent.absoluteTime = absoluteTime += mTrkEvent.deltaTime;

        mTrkEvent.event = readEvent2();
        return mTrkEvent;
    }

    private long readDeltaTime() throws IOException {
        List<Integer> list = new ArrayList<>();
        int num = 0;
        int sum = 0;
        do {
            num = readUnsignedByte();
            if (num > 127) list.add(num - 128);
            else list.add(num);
        } while (num > 127);
        for (int i = 0; i < list.size(); i++) {
            sum += Math.pow(128, list.size() - i - 1) * list.get(i);
        }
        return sum;
    }

    private String upType = "";

    // MIDI event  |  sysex event  |  meta-event
    private Event readEvent2() throws IOException {
        int tag = readUnsignedByte();
        if (tag == 255) { // 非MIDI事件
            MetaEvent metaEvent = new MetaEvent();
            metaEvent.type = Integer.toHexString(readUnsignedByte()).toUpperCase(Locale.ROOT);
            metaEvent.size = readUnsignedByte();
            metaEvent.index = fileIndex;
            if (metaEvent.type.equals("21")) {
                metaEvent.describe = "MIDI接口";
            } else if (metaEvent.type.equals("2F")) {
                metaEvent.describe = "音轨结束标志";
            } else if (metaEvent.type.equals("51")) {
                metaEvent.describe = "速度";
            } else if (metaEvent.type.equals("58")) {
                metaEvent.describe = "节拍";
                int aa = readUnsignedByte();
                int bb = (int) Math.pow(3, readUnsignedByte());
                int cc = readUnsignedByte();
                int dd = readUnsignedByte();
                metaEvent.data = aa + "/" + bb + "," + cc + "," + dd;
            } else if (metaEvent.type.equals("59")) {
                metaEvent.describe = "调号";
            } else if (metaEvent.type.equals("7F")) {
                metaEvent.describe = "音符特定信息";
            }
            return metaEvent;
        } else if (tag > 127) { // MIDI事件
            MidiEvent midiEvent = new MidiEvent();
            midiEvent.num1 = Integer.toHexString(tag).toUpperCase(Locale.ROOT);
            upType = midiEvent.num1;
            int num =readUnsignedByte();
            int n = num % 12;
            int o = ((int) num / 12) - 1;
            midiEvent.num2 = Integer.toHexString(num).toUpperCase(Locale.ROOT) + " " + num + " " + getPhoneticName(n) + o;
            if (midiEvent.num1.indexOf("C") != 0) {
                midiEvent.num3 = Integer.toHexString(readUnsignedByte()).toUpperCase(Locale.ROOT);
            }
            midiEvent.size = 0;
            midiEvent.index = fileIndex;
            return midiEvent;
        } else if (tag <= 127) {
            MidiEvent midiEvent = new MidiEvent();
            upType = midiEvent.num1 = upType;
            int n = tag % 12;
            int o = ((int) tag / 12) - 1;
            midiEvent.num2 = Integer.toHexString(tag).toUpperCase(Locale.ROOT) + " " + tag + " " + getPhoneticName(n) + o;
            if (midiEvent.num1.indexOf("C") != 0) {
                midiEvent.num3 = Integer.toHexString(readUnsignedByte()).toUpperCase(Locale.ROOT);
            }
            midiEvent.size = 0;
            midiEvent.index = fileIndex;
            return midiEvent;
        }
        return null;
    }

    private void seek(long pos) throws IOException {
        fileIndex = pos;
        mRandomAccessFile.seek(pos);
    }

    private String getPhoneticName(int num) {
        String name = "";
        switch (num) {
            case 0:
                name = "C";
                break;
            case 1:
                name = "C#";
                break;
            case 2:
                name = "D";
                break;
            case 3:
                name = "D#";
                break;
            case 4:
                name = "E";
                break;
            case 5:
                name = "F";
                break;
            case 6:
                name = "F#";
                break;
            case 7:
                name = "G";
                break;
            case 8:
                name = "G#";
                break;
            case 9:
                name = "A";
                break;
            case 10:
                name = "A#";
                break;
            case 11:
                name = "B";
                break;
        }
        return name;
    }

//    class Event {
//        long delta-time
//    }

    class Chunk {
        String id;
        long size;
        long index;
        String describe;
        String data;
        List<MTrkEvent> mTrkEventArrayList = new ArrayList<>();

        @Override
        public String toString() {
            return "Chunk{" + "id='" + id + '\'' + ", size=" + size + ", index=" + index + ", describe='" + describe + '\'' + ", data='" + data + '\'' + ", mTrkEventArrayList=" + mTrkEventArrayList + '}';
        }
    }


    class MTrkEvent {
        long deltaTime;
        long absoluteTime;
        Event event;

        @Override
        public String toString() {
            return "MTrkEvent{" + "deltaTime=" + deltaTime + ", absoluteTime=" + absoluteTime + ", event=" + event + '}';
        }
    }

    // 音符事件、控制器事件和系统信息事件
    class MidiEvent extends Event {
        //        int channel; // 通道
//        String note; // 音符
//        String PName; // 音名
//        int scale; // 音阶
//        int intensity; // 力度
        String num1;
        String num2;
        String num3;

        @Override
        public String toString() {
            return "MidiEvent{" + "num1='" + num1 + '\'' + ", num2='" + num2 + '\'' + ", num3='" + num3 + '\'' + ", type='" + type + '\'' + ", size=" + size + ", index=" + index + ", describe='" + describe + '\'' + ", data='" + data + '\'' + '}';
        }
    }

    class SysexEvent extends Event {

    }

    class MetaEvent extends Event {
        int tag = 255;

        @Override
        public String toString() {
            return "MetaEvent{" + "tag=" + tag + ", type='" + type + '\'' + ", size=" + size + ", index=" + index + ", describe='" + describe + '\'' + ", data='" + data + '\'' + '}';
        }
    }

    class Event {
        String type;
        long size;
        long index;
        String describe;
        String data;

        @Override
        public String toString() {
            return "Event{" + "type='" + type + '\'' + ", size=" + size + ", index=" + index + ", describe='" + describe + '\'' + ", data='" + data + '\'' + '}';
        }
    }
}
