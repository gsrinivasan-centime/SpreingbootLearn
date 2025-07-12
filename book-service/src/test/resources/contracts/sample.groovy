// Sample contract for build purposes
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Sample contract to satisfy maven build"
    request {
        method 'GET'
        url '/api/v1/books/health'
        headers {
            contentType(applicationJson())
        }
    }
    response {
        status OK()
        body([status: "OK"])
        headers {
            contentType(applicationJson())
        }
    }
}
