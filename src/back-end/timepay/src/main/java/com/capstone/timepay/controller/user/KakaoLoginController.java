package com.capstone.timepay.controller.user;

import com.capstone.timepay.domain.user.model.AuthenticationRequest;
import com.capstone.timepay.domain.user.model.AuthenticationResponse;

import com.capstone.timepay.service.user.dto.KakaoLoginDto;
import org.springframework.security.authentication.AuthenticationManager;
import com.capstone.timepay.service.user.service.KakaoLoginService;
import com.capstone.timepay.service.user.service.UserDetailService;
import com.capstone.timepay.utility.JwtUtils;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class KakaoLoginController {
    private final KakaoLoginService kakaoLoginService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailService userDetailService;

    /* 카카오 로그인 */
    @GetMapping("/login")
    @ApiOperation(value="카카오 간편 로그인 콜백 함수",notes = "리다이렉션 URI로 실행되는 함수입니다. 따로 사용 X")
    public ResponseEntity<?> kakaoCallback(@RequestParam String code) throws  Exception {
        String access_Token = kakaoLoginService.getKaKaoAccessToken(code);
        KakaoLoginDto data = kakaoLoginService.createKakaoUser(access_Token);
        String email = data.getEmail();

        final UserDetails userDetails = userDetailService.loadUserByUsername(email);
        final String token = jwtUtils.generateToken(userDetails);
        System.out.println(userDetails.getUsername());
        return ResponseEntity.ok(new AuthenticationResponse(token, userDetails));
        /* JWT 토큰 발급 */
        /* 카카오 로그인을 통해 리다이렉션 되는 URL에서 RequestBody를 설정해줄 수 없음 */
        /* 서비스 단에서 RequestBody를 생성하여 토큰을 생성해야할 것 같음 */

        // String name = authenticationRequest.getName();
        // authenticate(uid, authenticationRequest.getPassword());
        // final UserDetails userDetails = userDetailService.loadUserByUsername(name);
        // final String token = jwtUtils.generateToken(userDetails);
        // final User user = userDetailService.getUser(name);


        /* DB 작성 완료 후, uid 보내기 */
        // return ResponseEntity.ok(new AuthenticationResponse(token, user));
    }

    private void authenticate(Long uid, String email) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(uid, email));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }




}
