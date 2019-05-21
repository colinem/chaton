package server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrivateCommunication {

    private enum ClientType{A,B};
    private boolean hasFirstKey=false;
    private boolean hasSecondKey=false;
    private SelectionKey keyA;
    private SelectionKey keyB;
    static private int BUFFER_SIZE = 1_024;
    private ByteBuffer bbinA= ByteBuffer.allocate(BUFFER_SIZE);
    private ByteBuffer bboutA= ByteBuffer.allocate(BUFFER_SIZE);
    private ByteBuffer bbinB= ByteBuffer.allocate(BUFFER_SIZE);
    private ByteBuffer bboutB= ByteBuffer.allocate(BUFFER_SIZE);

    static private Logger logger = Logger.getLogger(PrivateCommunication.class.getName());

    private boolean killed=false;

    public void setKeyA(SelectionKey keyA) {
        this.keyA = keyA;
        
    }

    public void setKeyB(SelectionKey keyB) {
        this.keyB = keyB;
    }

    static private class Context{

        final private SelectionKey key;
        final private SocketChannel sc;
        final private ByteBuffer bbin;
        final private ByteBuffer bbout;
        final private PrivateCommunication server;
        private boolean closed = false;
        private String login;

        private Context(PrivateCommunication server, SelectionKey key, ClientType clientType){
            if(clientType.equals(ClientType.A)){
                bbin=server.bbinA;
                bbout=server.bboutB;
            }else {
                bbin=server.bbinB;
                bbout=server.bboutA;
            }
            this.key = key;
            this.sc = (SocketChannel) key.channel();
            this.server = server;
        }

        /**
         * Process the content of bbin
         *
         * The convention is that bbin is in write-mode before the call
         * to process and after the call
         *
         */
        private void process() {

            bbin.flip();
            while (bbin.remaining()>=Byte.BYTES && bbout.remaining()>=Byte.BYTES){
                bbout.put(bbin.get());
            }
            bbin.compact();
        }

        /**
         * Add a message to the message queue, tries to fill bbOut and updateInterestOps
         *
         * @param msg
         */
        private void queueMessage(ByteBuffer msg) {

        }



        /**
         * Update the interestOps of the key looking
         * only at values of the boolean closed and
         * of both ByteBuffers.
         *
         * The convention is that both buffers are in write-mode before the call
         * to updateInterestOps and after the call.
         * Also it is assumed that process has been be called just
         * before updateInterestOps.
         */

        private void updateInterestOps() {
//			System.out.println("updateInterestOps : closed ? " + closed
//					+ "\n ; bbin.remaining = " + bbin.remaining()
//					+ "\n ; bbbin.position = " + bbin.position());
            var interestOps = 0;
            if (!closed && bbin.hasRemaining())
                interestOps = SelectionKey.OP_READ;
            if (bbout.position() != 0)
                interestOps |= SelectionKey.OP_WRITE;
            if (interestOps == 0){
                silentlyClose();
            }
            else
                key.interestOps(interestOps);

        }

        private void silentlyClose() {
//			System.out.println(login + " : silentlyClose");
            try {
                server.clients.clear();
                sc.close();
            } catch (IOException e) {
                // ignore exception
            }
        }

        /**
         * Performs the read action on sc
         *
         * The convention is that both buffers are in write-mode before the call
         * to doRead and after the call
         *
         * @throws IOException
         */
        private void doRead() throws IOException {
            if (sc.read(bbin) == -1)
                closed = true;
            process();
            updateInterestOps();
        }

        /**
         * Performs the write action on sc
         *
         * The convention is that both buffers are in write-mode before the call
         * to doWrite and after the call
         *
         * @throws IOException
         */

        private void doWrite() throws IOException {
//			System.out.println("doWrite");
//			System.out.println("bbout = " + bbout);
            sc.write(bbout.flip());
            bbout.compact();
//			System.out.println("bbout = " + bbout);
            process();
            updateInterestOps();
        }



    }


    private void treatKey(SelectionKey key) {
        printSelectedKey(key); // for debug
        try {
            if (key.isValid() && key.isAcceptable()) {
                doAccept(key);
            }
        } catch(IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
        try {
            if (key.isValid() && key.isWritable()) {
                ((Context) key.attachment()).doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                ((Context) key.attachment()).doRead();
            }
        } catch (IOException e) {
            logger.log(Level.INFO,"Connection closed with client due to IOException",e);
            silentlyClose(key);
        }


    }
    private void doAccept(SelectionKey key) throws IOException {
        var socket = serverSocketChannel.accept();
        if (socket != null) {
            socket.configureBlocking(false);
            var newKey = socket.register(selector, SelectionKey.OP_READ);
            newKey.attach(new Context(this, newKey, ClientType.A));
        }
    }
    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    public void process(){
        while (!Thread.interrupted() && !killed){
            treatKey(keyA);
            treatKey(keyB);
        }
    }

}
