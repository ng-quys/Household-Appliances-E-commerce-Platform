package com.example.WebBanDoGiaDung.controller;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/news")
public class NewsController {

    @GetMapping
    public String tinTuc(Model model) {
        model.addAttribute("news", List.of());
        model.addAttribute("notice", "Project ASP.NET cũ lấy tin từ NewsServices, nhưng phần service/model news chưa nằm trong DB entity JPA hiện tại.");
        return "news/index";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Integer id, Model model) {
        model.addAttribute("newsId", id);
        model.addAttribute("notice", "Cần tạo NewsService/NewsArticle riêng nếu muốn chuyển nốt module tin tức.");
        return "news/details";
    }
}
