package com.github.zxbu.webdavteambition;

import com.github.zxbu.webdavteambition.store.TeambitionFileSystemStore;
import net.sf.webdav.LocalFileSystemStore;
import net.sf.webdav.WebdavServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.ErrorPageFilter;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootApplication
public class WebdavTeambitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebdavTeambitionApplication.class, args);
    }

    @Bean
    public ServletRegistrationBean<WebdavServlet> myServlet(){
        ServletRegistrationBean<WebdavServlet> servletRegistrationBean = new ServletRegistrationBean<>(new WebdavServlet(), "/*");
        Map<String, String> inits = new LinkedHashMap<>();
        inits.put("ResourceHandlerImplementation", TeambitionFileSystemStore.class.getName());
//        inits.put("ResourceHandlerImplementation", LocalFileSystemStore.class.getName());
        inits.put("rootpath", "./");
        inits.put("storeDebug", "1");
        servletRegistrationBean.setInitParameters(inits);
        return servletRegistrationBean;
    }

    @Bean
    public ErrorPageFilter errorPageFilter() {
        return new ErrorPageFilter();
    }

    @Bean
    public FilterRegistrationBean disableSpringBootErrorFilter(ErrorPageFilter filter) {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(filter);
        filterRegistrationBean.setEnabled(false);
        return filterRegistrationBean;
    }

}
