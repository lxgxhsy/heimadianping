package com.example.heimadianping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@MapperScan("com.example.heimadianping.mapper")
public class HeimadianpingApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeimadianpingApplication.class, args);
	}

}
