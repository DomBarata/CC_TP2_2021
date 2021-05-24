import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.Condition;

public class Pedido {
    private final String fileName;
    private final String server;
    private final LocalDateTime horas;


    public Pedido(String filename, String server, LocalDateTime horas) {
        this.fileName = filename;
        this.server = server;
        this.horas = horas;
    }

    public Pedido(String filename, String server) {
        this.fileName = filename;
        this.server = server;
        this.horas = LocalDateTime.now();
    }

    public String getFilename(){
        return this.fileName;
    }

    public String getServer(){
        return this.server;
    }

    public Long getTime() {
        return this.horas.until(LocalDateTime.now(), ChronoUnit.SECONDS);
    }

}
