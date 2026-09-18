package com.healthdiet.service;

import com.healthdiet.dto.FoodProcessResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class EmailServiceImpl implements EmailService {

    // ✅ 只改了这里，加了 required = false
    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String sendMailFrom;

    private final Map<String, String> codeMap = new ConcurrentHashMap<>();
    private final Map<String, Long> expireMap = new ConcurrentHashMap<>();

    @Override
    public void sendVerifyCode(String email) {
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        codeMap.put(email, code);
        expireMap.put(email, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));

        // ✅ 安全判断：如果没配置邮件就不发送，不报错
        if (javaMailSender == null) {
            System.err.println("【警告】邮件未配置，验证码：" + code);
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sendMailFrom);
            helper.setTo(email);
            helper.setSubject("健康饮食平台-邮箱验证码");
            helper.setText("您好，您的验证码为：<h3>" + code + "</h3>，有效期5分钟", true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败");
        }
    }

    @Override
    public boolean checkCode(String email, String code) {
        if (!expireMap.containsKey(email) || System.currentTimeMillis() > expireMap.get(email)) {
            codeMap.remove(email);
            expireMap.remove(email);
            return false;
        }
        return code.equals(codeMap.get(email));
    }

    @Service
    @RequiredArgsConstructor
    public static class FoodProcessService {

        private final DoubaoCutoutService cutoutService;
        private final BaiduDishRecognitionService baiduService;
        private final AiNutritionService aiNutritionService;

        /**
         * 根据图片生成食物识别与营养估算，不入库
         */
        public FoodProcessResult processFood(String imageBase64, Integer userId) {
            // 1. 抠图
            String stickerBase64 = cutoutService.cutout(imageBase64);

            // 2. 百度识别
            String dishName = baiduService.recognizeDishName(imageBase64);

            // 3. 营养估算
            AiNutritionService.FoodNutritionEstimate estimate = aiNutritionService.estimateFoodNutrition(dishName, null, null);

            // 4. 返回 DTO
            return FoodProcessResult.builder()
                    .stickerBase64(stickerBase64)
                    .dishName(dishName)
                    .calories(estimate.getCalories())
                    .carbG(estimate.getCarbG())
                    .proteinG(estimate.getProteinG())
                    .fatG(estimate.getFatG())
                    .fiberG(estimate.getFiberG())
                    .sugarG(estimate.getSugarG())
                    .sodiumMg(estimate.getSodiumMg())
                    .tips(estimate.getTips())
                    .ingredient("")
                    .build();
        }
    }
}