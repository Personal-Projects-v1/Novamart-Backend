package commerce.novamart.order.controller;

import commerce.novamart.common.dto.ApiResponse;
import commerce.novamart.common.security.CurrentUser;
import commerce.novamart.order.dto.OrderDtos.OrderResponse;
import commerce.novamart.order.dto.OrderDtos.PlaceOrderRequest;
import commerce.novamart.order.dto.OrderDtos.ReturnItemsRequest;
import commerce.novamart.order.dto.OrderDtos.ReturnResponse;
import commerce.novamart.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orders;
    private final CurrentUser currentUser;

    @PostMapping
    @Operation(summary = "Place an order (guest checkout allowed; signed-in orders are linked to the account)")
    public ResponseEntity<ApiResponse<OrderResponse>> place(@Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse order = orders.place(currentUser.find().orElse(null), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Order placed", order));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> mine() {
        return ApiResponse.success(orders.mine(currentUser.require()));
    }

    @GetMapping("/track")
    @Operation(summary = "Track an order by order number and the email or phone used at checkout")
    public ApiResponse<OrderResponse> track(@RequestParam String orderNumber, @RequestParam String contact) {
        return ApiResponse.success(orders.track(orderNumber, contact));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> get(@PathVariable String id) {
        return ApiResponse.success(orders.get(currentUser.require(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable String id) {
        return ApiResponse.success("Order cancelled", orders.cancel(currentUser.require(), id));
    }

    @PostMapping("/{id}/returns")
    public ResponseEntity<ApiResponse<ReturnResponse>> requestReturn(@PathVariable String id,
                                                                    @Valid @RequestBody ReturnItemsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Return requested", orders.requestReturn(currentUser.require(), id, request)));
    }
}
