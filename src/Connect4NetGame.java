/**
 * Connect4NetworkGame.java
 * ClientHandler inner class that is threaded
*/

import java.lang.Thread;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;


public class Connect4NetGame {

    private Connect4Logic game;

    public Connect4NetGame(Connect4Logic game) {
        this.game = game;
    }

    // unsure if this should be synchronized since I use a synchronized block
    //public synchronized boolean isValidMove(char mark, int column) {
    public boolean isValidMove(char mark, int column) {
        return mark == game.getCurrentMove() 
               &&
               game.verifyMove(column);
    }
    


    public class ClientHandler extends Thread {

        private char mark;
        private BufferedReader in;
        private PrintWriter out;
        private Socket socket;
        private ClientHandler opponent;
        private String name;

        public ClientHandler(Socket socket, char mark, String name) {
            this.socket = socket;
            this.mark = mark;
            this.name = name;

            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("WELCOME " + mark);
                out.println("MESSAGE Waiting for other player to connect");
                updateIndicator();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }

        public void setOpponent(ClientHandler opponent) {
            this.opponent = opponent;
        }


        public void updateIndicator() {
            out.println("SET " + game.getCurrentMove());
        }


        // handles the other player move message
        public void opponentMoved(int column, int row) {
            System.out.println(name + " " + "OPPONENT_MOVED" + " " + column + " " + row);
            out.println("OPPONENT_MOVED" + " " + column + " " + row);
            String result = game.isWin() ? "DEFEAT" : game.isDraw() ? "DRAW" : "";
            out.println(result);
        }


        public void run() {
            try {
                out.println("MESSAGE Players have connected, the game will begin now");
                if (mark == game.getCurrentMove()) {
                    out.println("MESSAGE It it your turn");
                }

                while (true) {
                    String clientMessage = in.readLine();

                    if (clientMessage.startsWith("MOVE")) {
                        int column = Integer.parseInt(clientMessage.substring(5));
                        synchronized(this) {
                            if (isValidMove(mark, column)) {
                                int row = game.makeMove(column);
                                game.switchTurns();
                                out.println("VALID_MOVE " + column + " " + row);
                                opponent.opponentMoved(column, row);
                                updateIndicator();
                                opponent.updateIndicator();
                                String gameOver = game.isWin() ? "VICTORY" : game.isDraw() ? "DRAW" : "";
                                out.println(gameOver);
                            }
                        }
                    } else if (clientMessage.startsWith("QUIT")) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Client lost connection");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {}
            }
        }

    }

}