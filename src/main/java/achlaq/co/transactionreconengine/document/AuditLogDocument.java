package achlaq.co.transactionreconengine.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Document(indexName = "audit_logs")
public class AuditLogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String requestId;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String action;

    @Field(type = FieldType.Keyword)
    private String riskLevel;

    @Field(type = FieldType.Text)
    private String metadata;

    @Field(type = FieldType.Double)
    private BigDecimal amountSnapshot;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime timestamp;
}