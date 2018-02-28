package com.yazaki.yazaki.ui.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.yazaki.yazaki.domain.exception.DateIllegalArgumentException;
import com.yazaki.yazaki.domain.exception.OrderIllegalArgumentException;
import com.yazaki.yazaki.domain.exception.RecordNotFoundException;
import com.yazaki.yazaki.domain.model.Dish;
import com.yazaki.yazaki.domain.model.DishCounter;
import com.yazaki.yazaki.domain.model.Order;
import com.yazaki.yazaki.domain.service.dish.DishService;
import com.yazaki.yazaki.domain.service.order.OrderService;
import com.yazaki.yazaki.ui.form.DailyMenuForm;
import com.yazaki.yazaki.ui.form.StatisticForm;

@RestController
public class OrderRestController {

    private static final Logger logger = LoggerFactory.getLogger(OrderRestController.class);

    private final DishService dishService;
    private final OrderService orderService;

    @Autowired
    public OrderRestController(final DishService dishService, final OrderService orderService) {
        this.dishService = dishService;
        this.orderService = orderService;
    }

    @GetMapping("/daily-menu")
    public ResponseEntity<List<Dish>> getAllDishes() {
        logger.info("Order rest controller find all dishes");
        List<Dish> dishes = dishService.getAllDishes();

        return ResponseEntity.status(HttpStatus.OK).body(dishes);
    }

    @PostMapping("/daily-menu/save")
    public ResponseEntity<Void> saveDailyMenu(@RequestBody @Valid final DailyMenuForm dailyMenuForm, final BindingResult result) {
        
    	if(result.hasErrors()) {
    		final String errorMsg = buildErrorMsg(result);
    		throw new OrderIllegalArgumentException(errorMsg);
    	}
    	
    	logger.info("Execute operation for saving the daily menu");
        final Order order = convertToOrderWithRelationShips(dailyMenuForm);
        orderService.saveOrder(order);
        logger.info("Successfuly saved order.");
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
	@GetMapping("/statistics")
	public ResponseEntity<Order> getStatisticsFromSelectedDate(@Valid final StatisticForm statisticForm, final BindingResult result) {
		
		if(result.hasErrors()) {
			final String errorMsg = buildErrorMsg(result);
			throw new DateIllegalArgumentException(errorMsg);
		}
		
		final LocalDate date = LocalDate.of(statisticForm.getYear(), statisticForm.getMonth(), statisticForm.getDay());
		final Order order = orderService.findOrderByDate(date);

		if(Objects.isNull(order)) {
			throw new RecordNotFoundException("Няма намерени записи за тази дата.");
		}
		
		return new ResponseEntity<Order>(order, HttpStatus.OK);
	}

	private String buildErrorMsg(final BindingResult result) {
		logger.error("Illegal arguments");
		StringBuilder errorMessageBuilder = new StringBuilder();
		
		if(result.getFieldErrorCount() > 1) {
			
			result.getAllErrors().stream().forEach(error -> {
				errorMessageBuilder.append(error.getDefaultMessage());
				errorMessageBuilder.append(System.lineSeparator());
			});
			
		} else {
			errorMessageBuilder.append(result.getFieldError().getDefaultMessage());
		}
		
		return errorMessageBuilder.toString();
	}

    private Order convertToOrderWithRelationShips(final DailyMenuForm dailyMenuForm) {
        final Order order = new Order();
        List<DishCounter> dishCounters = new ArrayList<>();

        order.setDate(LocalDate.of(dailyMenuForm.getYear(), dailyMenuForm.getMonth(), dailyMenuForm.getDay()));
        
        dailyMenuForm.getDishIds().forEach(id -> {
        	final Dish dish = dishService.findDishById(id);
        	final DishCounter dishCounter = new DishCounter();
        	
        	dishCounter.setDish(dish);
        	dishCounter.setCounter(0L);
        	dishCounter.setOrder(order);
        	
        	dishCounters.add(dishCounter);
        });
        
        order.setDishCounters(dishCounters);
        
		return order;
    }
}
