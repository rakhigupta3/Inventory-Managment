/**
 * 
 */
package com.sellar.managment.fms.order;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.omg.IOP.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.sellar.managment.fms.FMSCustomException;
import com.sellar.managment.fms.inventory.InventoryService;
import com.sellar.managment.fms.inventory.domain.ProductStock;
import com.sellar.managment.fms.order.domain.GeneratedOrderDetails;
import com.sellar.managment.fms.order.domain.OrderDetail;
import com.sellar.managment.fms.order.domain.OrderTransportDetail;
import com.sellar.managment.fms.order.domain.OrderWrapper;
import com.sellar.managment.fms.order.domain.OrderedProduct;
import com.sellar.managment.fms.transaction.PaymentService;
import com.sellar.managment.fms.transaction.domain.PaymentDetail;
import com.sellar.managment.fms.user.UserService;
import com.sellar.managment.fms.util.EmailService;
import com.sellar.managment.fms.util.FMSConstant;
import com.sellar.managment.fms.util.FPSUtility;

/**
 * @author rakumari
 *
 */
@Service(value="fmsService")
public class OrderServiceImpl implements OrderService{
	
	@Autowired
	OrderDAO orderDao;
	
	@Autowired
	InventoryService inventoryService;
	
	@Autowired
	PaymentService paymentService;
	
	@Autowired
	EmailService emailServie;
	
	
	
	
	

	@Override
	public void saveOrderDetail(OrderWrapper orderWrapper,Map userMap) {
		
	try{	
		OrderDetail orderDetail = orderWrapper.getOrder();
		Short compType = (Short) userMap.get(FMSConstant.USER_COMPANY);
		String userName = (String)userMap.get(FMSConstant.USER_NAME);
		
		saveOrder(orderDetail,userName);
		
		for(OrderedProduct product : orderWrapper.getOrderedProductList()){
			product.setOrderId(orderDetail.getOrderId());
			product.setCreatedBy(userName);
			product.setUpdBy(userName);
			product.setCreatedOn(new Date());
			product.setUpdOn(new Date());
			orderDao.saveOrderdProduct(product);
			ProductStock prodStock = inventoryService.getProductStockByStockId(product.getStockId());
			float quantity = prodStock.getQuantity()-product.getQuantity();
			prodStock.setQuantity(quantity);
			inventoryService.saveStockDetails(prodStock,compType,userName);
		}
		OrderTransportDetail transport = orderWrapper.getTransportDetail();
		transport.setOrderId(orderDetail.getOrderId());
		saveTransportDetail(transport,userName);
		emailServie.SendEmail("Order Generation Notification", getEmailMessage(orderDetail), "prasadfertilizernokha@gmail.com");
	}catch(Exception e){
		throw new FMSCustomException("Error while saving order" + e.getMessage());
	}
		
		
		
		
	}

	private void saveTransportDetail(OrderTransportDetail transportDetail,String userName) {
		// TODO Auto-generated method stub
		if(transportDetail.getTransportDetailId()==0){
			
			transportDetail.setCreatedBy(userName);
			transportDetail.setUpdBy(userName);
			transportDetail.setCreatedOn(new Date());
			transportDetail.setUpdOn(new Date());
			
		}else{
			transportDetail.setUpdBy(userName);
			transportDetail.setUpdOn(new Date());
		}
		try{
			orderDao.saveOrderTransportDetail(transportDetail);
		}catch(Exception e){
			throw new FMSCustomException("Error while saving order transport detail"+ e.getMessage());
		}
		
	}

	@Override
	public List<GeneratedOrderDetails> getOrderDetailsList() {
		// TODO Auto-generated method stub
		List<GeneratedOrderDetails> generatedOrderList = new ArrayList<>();
		List<OrderDetail>	orderDetailList = orderDao.getAllOrderDetails();
		for(OrderDetail order : orderDetailList){
			GeneratedOrderDetails generatedOrder = new GeneratedOrderDetails();
			generatedOrder.setOrder(order);
			generatedOrder.setOrderedProductList(orderDao.getAllOrderdProductByOrderId(order.getOrderId()));
			generatedOrder.setPaymentDetailList(paymentService.getPaymentInfoByOrderId(order.getOrderId()));
			generatedOrder.setTransportDetail(orderDao.getTransportDetailByOrderId(order.getOrderId()));
			generatedOrderList.add(generatedOrder);
		}
		return generatedOrderList;
	}

	@Override
	public List<OrderedProduct> getOrderedProductList(int orderId) {
		// TODO Auto-generated method stub
		return orderDao.getAllOrderdProductByOrderId(orderId);
	}

	@Override
	public OrderDetail getOrderDetailsByOrderId(int orderId) {
		// TODO Auto-generated method stub
		return orderDao.getOrderDetailsByOrderId(orderId);
	}

	@Override
	public void saveOrder(OrderDetail orderDetail,String userName) {
		// TODO Auto-generated method stub
		if(orderDetail.getOrderId()==0){
			
			orderDetail.setCreatedBy(userName);
			orderDetail.setUpdBy(userName);
			orderDetail.setCreatedOn(new Date());
			orderDetail.setUpdOn(new Date());
			orderDetail.setOrderStatusTypeId(FMSConstant.ORDERSTATUSTYPE_INITIATED);
			orderDetail.setPendingAmount(orderDetail.getOrderPrice());
			orderDetail.setPaymentStatus(false);
		}else{
			orderDetail.setUpdBy(userName);
			orderDetail.setUpdOn(new Date());
		}
		try{
			orderDao.saveOrderDetail(orderDetail);
		}catch(Exception e){
			throw new FMSCustomException("Error while saving order  detail"+ e.getMessage());
		}
		
	}

	@Override
	public void cancelOrderByOrderId(int orderId,Map userMap) {
		// TODO Auto-generated method stub
		Short compType = (Short) userMap.get(FMSConstant.USER_COMPANY);
		String userName = (String) userMap.get(FMSConstant.USER_NAME);
		OrderDetail order = orderDao.getOrderDetailsByOrderId(orderId);
		List<OrderedProduct> orderedProductList = orderDao.getAllOrderdProductByOrderId(orderId);
		for(OrderedProduct orderedProduct :  orderedProductList){
			float updatedQuan = orderedProduct.getQuantity() + orderedProduct.getProductStock().getQuantity();
			orderedProduct.getProductStock().setQuantity(updatedQuan);
			inventoryService.saveStockDetails(orderedProduct.getProductStock(),compType,userName);
			
		}
		order.setOrderStatusTypeId(FMSConstant.ORDERSTATUSTYPE_CANCELLED);
		order.setUpdBy(userName);
		order.setUpdOn(new Date());
		orderDao.saveOrderDetail(order);
	}

	@Override
	public String getOrderNumber(Short compType) {
		// TODO Auto-generated method stub
		Integer orderId = orderDao.getLastOrderId();
		return FPSUtility.generateOrderNumber(orderId,compType);
	}
	
	public String getEmailMessage(OrderDetail order){
		
		String emailText = "Dear Saler,"+
							"\n\nOne new order of challan number"+ order.getChallanNumber() + " ,challan date" + order.getChallanDate()+
							"Total order price is "+order.getOrderTotalPrice()+
							"To retailer"+order.getRetailerDetail().getRetailerFirstName()+" "+ order.getRetailerDetail().getRetailerLastName()+
							"has been generated successfully ";
		
		return emailText;
		
	}


}
