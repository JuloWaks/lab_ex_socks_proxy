import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Pipe extends Thread {
    InputStream inputStream;
    OutputStream outputStream;
    boolean canRead = true;
    public Pipe(InputStream in, OutputStream out)
    {
        inputStream = in;
        outputStream = out;

    }
    public void run(){

        while (canRead)
        {
            try {
                IOUtils.copy();

            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                try {
                    inputStream.close();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.err.println("Had error while piping"  );
                canRead = false;
            }
        }

    }
}
