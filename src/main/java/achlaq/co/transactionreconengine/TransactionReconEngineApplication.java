package achlaq.co.transactionreconengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TransactionReconEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionReconEngineApplication.class, args);
    }

}
