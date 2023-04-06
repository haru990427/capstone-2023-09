package com.capstone.timepay.service.user.service;

import com.capstone.timepay.domain.user.User;
import com.capstone.timepay.domain.user.UserRepository;
import com.capstone.timepay.service.user.dto.KakaoLoginDto;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;



@Service
@RequiredArgsConstructor
public class KakaoLoginService {
    private final UserRepository userRepository;
    private final Environment env;

    public  String getKaKaoAccessToken(String code){
        String access_Token="";
        String refresh_Token ="";
        String line = "";
        String result = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";
        try{
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code");
            sb.append("&client_id=79587b639a3a9ca1c9433fa63bc55863");
            sb.append("&redirect_uri=http://localhost:8080/login");
            sb.append("&code=" + code);
            bw.write(sb.toString());
            bw.flush();

            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);

            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

            access_Token = element.getAsJsonObject().get("access_token").getAsString();
            refresh_Token = element.getAsJsonObject().get("refresh_token").getAsString();

            System.out.println("access_token : " + access_Token);
            System.out.println("refresh_token : " + refresh_Token);

            br.close();
            bw.close();
        }catch (IOException e) {
            e.printStackTrace();
        }

        return access_Token;
    }

    public  KakaoLoginDto createKakaoUser(String token) { // throws BaseException 오류나와서 보류
        String reqURL = "https://kapi.kakao.com/v2/user/me";
        String email = null;
        String sex = null;
        String birthday = null;
        Long id = null;
        KakaoLoginDto user = new KakaoLoginDto();

        //access_token을 이용하여 사용자 정보 조회
        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + token); //전송할 header 작성, access_token전송

            //결과 코드가 200이라면 성공
            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            //요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);

            //Gson 라이브러리로 JSON파싱
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

            id = element.getAsJsonObject().get("id").getAsLong();

            /* uid값 비교하여 중복된 데이터는 데이터베이스에 저장X */
            if(userRepository.findByUid(id) == null)
            {
                /* 이메일 제공 여부 확인 및 이메일 가져오기 */
                boolean hasEmail = element.getAsJsonObject().get("kakao_account").getAsJsonObject().get("has_email").getAsBoolean();
                if (hasEmail) {
                    email = element.getAsJsonObject().get("kakao_account").getAsJsonObject().get("email").getAsString();

                } else {
                    email = "이메일 동의하지 않음";
                }

                /* 성별 제공 여부 확인 및 성별 가져오기 */
                boolean hasSex = element.getAsJsonObject().get("kakao_account").getAsJsonObject().get("has_gender").getAsBoolean();
                if (hasSex) {
                    sex = element.getAsJsonObject().get("kakao_account").getAsJsonObject().get("gender").getAsString();
                } else {
                    sex = "성별 동의하지 않음";
                }

                /* 생일 제공 여부 확인 및 생일 가져오기 */
                boolean hasBirthday = element.getAsJsonObject().get("kakao_account").getAsJsonObject().get("has_birthday").getAsBoolean();
                if (hasBirthday) {
                    birthday = element.getAsJsonObject().get("kakao_account").getAsJsonObject().get("birthday").getAsString();
                } else {
                    birthday = "생일 동의하지 않음";
                }

            } else {
                System.out.println("\n이미 저장된 데이터래요~\n");
                User userTmp = userRepository.findByUid(id);
                user = KakaoLoginDto.toKaKaoLoginDto(userTmp);
                kakaoLoginJWT(user);
            }


            System.out.println("id : " + id);
            System.out.println("email : " + email);
            System.out.println("gender : " + sex);
            System.out.println("birthday : " + birthday);

            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return user;
    }

    /* 데이터베이스 추가 작업 */
    public KakaoLoginDto createKakaoUsers(Long id, String email, String sex){
        User user = new User();
        user.setUid(id);
        user.setEmail(email);
        user.setSex(sex);
        userRepository.save(user);

        return KakaoLoginDto.toKaKaoLoginDto(user);
    }

    public void kakaoLoginJWT(KakaoLoginDto userData){
        String userID = Long.toString(userData.getId());
        String token = JWT.create()
                .withSubject("JwtToken")
                .withExpiresAt(new Date(System.currentTimeMillis() + Long.parseLong(env.getProperty("token.expiration_time"))))
                .withClaim("email", userData.getEmail())
                .sign(Alorithm.HMAC512(env.getProperty("token.secret")));
    }
}
