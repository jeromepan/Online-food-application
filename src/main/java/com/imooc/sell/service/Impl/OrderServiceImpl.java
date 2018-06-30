package com.imooc.sell.service.Impl;

import com.imooc.sell.dataObject.OrderDetail;
import com.imooc.sell.dataObject.OrderMaster;
import com.imooc.sell.dataObject.ProductInfo;
import com.imooc.sell.dto.CartDTO;
import com.imooc.sell.dto.OrderDTO;
import com.imooc.sell.enums.OrderStatusEnum;
import com.imooc.sell.enums.PayStatusEnum;
import com.imooc.sell.enums.ResultEnum;
import com.imooc.sell.exception.SellException;
import com.imooc.sell.repository.OrderDetailRepository;
import com.imooc.sell.repository.OrderMasterRepository;
import com.imooc.sell.service.OrderService;
import com.imooc.sell.service.ProductService;
import com.imooc.sell.util.KeyGenerateUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import sun.security.util.KeyUtil;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private OrderMasterRepository orderMasterRepository;

    @Override
    @Transactional//如果存在扣库存失败等，要回滚
    //仍然可能超卖，比如很多人同时访问，读出来有库存，购买后库存不够了，这种情况可以用redis的锁机制来避免
    public OrderDTO create(OrderDTO orderDTO) {

        String orderId = KeyGenerateUtil.genUniqueKey();
        BigDecimal orderAmount = new BigDecimal(BigInteger.ZERO);

        //1. 查询商品（数量，价格，一定不能从前端调用关键数据！）
        for(OrderDetail orderDetail: orderDTO.getOrderDetailList()){
            Optional<ProductInfo> productInfo = productService.findOne(orderDetail.getProductId());
            if (productInfo == null) {
                throw new SellException(ResultEnum.PRODUCT_NOT_EXIST);
            }
            //2. 计算订单总价
            orderAmount = productInfo.get().getProductPrice()
                    .multiply(new BigDecimal(orderDetail.getProductQuantity()))
                    .add(orderAmount);

            //3. 订单详情入库
            BeanUtils.copyProperties(productInfo.get(),orderDetail);
            orderDetail.setDetailId(KeyGenerateUtil.genUniqueKey());
            orderDetail.setOrderId(orderId);
            //Spring提供的方法，会帮助拷贝对象的属性
            orderDetailRepository.save(orderDetail);
        }

        //3. 写入订单数据库（OrderMaster 和 OrderDetail）
        OrderMaster orderMaster = new OrderMaster();
        BeanUtils.copyProperties(orderDTO, orderMaster); //会覆盖！！先先拷贝，再改值，如果是null也会被拷贝进去
        orderMaster.setOrderId(orderId);
        orderMaster.setOrderAmount(orderAmount);
        orderMaster.setOrderStatus(OrderStatusEnum.NEW.getCode());
        orderMaster.setPayStatus(PayStatusEnum.WAIT.getCode());
        orderMasterRepository.save(orderMaster);

        //4. 扣库存
        //遍历的话可以使用类似scala语法的lambda表达式
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream().map(e -> new CartDTO(e.getProductId(),e.getProductQuantity())).collect(Collectors.toList());
        productService.decreaseStock(cartDTOList);

        return orderDTO;
    }

    @Override
    public OrderDTO findOne(String orderId) {

        Optional<OrderMaster> orderMaster = orderMasterRepository.findById(orderId);
        if (orderMaster == null){
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }

        List<OrderDetail> orderDetailList = orderDetailRepository.findByOrderId(orderId);
        if(CollectionUtils.isEmpty(orderDetailList)){
            throw new SellException(ResultEnum.ORDERDETAIL_NOT_EXIST);
        }

        OrderDTO orderDTO = new OrderDTO();
        BeanUtils.copyProperties(orderMaster.get(),orderDTO);
        orderDTO.setOrderDetailList(orderDetailList);

        return orderDTO;
    }

    @Override
    public Page<OrderDTO> findList(String buyerOpenid, Pageable pageable) {
        return null;
    }

    @Override
    public OrderDTO cancel(OrderDTO orderDTO) {
        return null;
    }

    @Override
    public OrderDTO finish(OrderDTO orderDTO) {
        return null;
    }

    @Override
    public OrderDTO paid(OrderDTO orderDTO) {
        return null;
    }
}
