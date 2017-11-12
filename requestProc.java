import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;


public class requestProc implements Runnable {

    private Socket socket;
    private InputStream in = null;
    private OutputStream out = null;
    private String DEFAULT_DIR = "/files";

    public requestProc(Socket socket) throws IOException {
        this.socket = socket;
        init();
    }

    private void init() throws IOException {
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    private String readHTTPHeader() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();
        String str;
        while (true) {
            str = reader.readLine();
            if (str == null || str.isEmpty()) {
                break;
            }
            builder.append(str + System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    private String getURL(String header) {
        int from = header.indexOf(" ") + 1;
        int to = header.indexOf(" ", from);
        String url = header.substring(from, to);
        int paramIndex = url.indexOf("?");
        if (paramIndex != -1) {
            url = url.substring(0, paramIndex);
        }
        return DEFAULT_DIR + url;
    }

    private int send(String url) throws IOException {
        InputStream strm = Server.class.getResourceAsStream(url);
        int code = (strm != null) ? 200 : 404;
        String header = writeHeader(code);
        PrintStream answer = new PrintStream(out, true, "UTF-8");
        answer.print(header);
        if (code == 200) {
            int count = 0;
            byte[] buffer = new byte[1024];
            while((count = strm.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            strm.close();
        }
        return code;
    }

    private String writeAnswer(int code) {

        if (code == 200){
            return "OK";
        }
        else if (code == 404){

            return "Not Found";
        }
        else return "Internal Server Error";

    }

    private String writeHeader(int code) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("HTTP/1.1 " + code + " " + writeAnswer(code) + "\n");
        buffer.append("\n");
        return buffer.toString();
    }

    public void run() {
        try {
            String header = readHTTPHeader();
            System.out.println(header + "\n");
            String url = getURL(header);
            System.out.println("Requested file: " + url + "\n");
            int code = send(url);
            System.out.println("Result code: " + code + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
