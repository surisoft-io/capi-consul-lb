package io.surisoft.capi.lb.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsulObject {

    @JsonProperty("ID")
    private String ID;

    @JsonProperty("ServiceName")
    private String serviceName;

    @JsonProperty("ServiceTags")
    private List<String> serviceTags;

    @JsonProperty("ServiceAddress")
    private String serviceAddress;

    @JsonProperty("ServiceID")
    private String serviceId;

    @JsonProperty("ServicePort")
    private int servicePort;
}
