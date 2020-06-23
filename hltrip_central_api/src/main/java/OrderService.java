import com.huoli.trip.common.vo.request.OrderStatusRequest;

/**
 * 中台订单相关dubbo服务接口定义
 */
public interface OrderService {
    Object getOrderStatus(OrderStatusRequest request);

}
