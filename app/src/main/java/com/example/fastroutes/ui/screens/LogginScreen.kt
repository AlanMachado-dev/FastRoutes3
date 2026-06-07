package com.example.fastroutes.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastroutes.R
import com.example.fastroutes.data.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.automirrored.rounded.ArrowForward

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val authRepository = remember {
        AuthRepository()
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var showPassword by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF031225),
                        Color(0xFF06366F),
                        Color(0xFF031225)
                    )
                )
            )
    ) {
        RouteBackground()


        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            Spacer(modifier = Modifier.height(72.dp))

            Image(
                painter = painterResource(id = R.drawable.fastroutes_icon),
                contentDescription = "FastRoutes",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false
                    )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "FastRoutes",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(34.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(34.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bienvenido",
                        color = Color(0xFF061B3A),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Inicia sesión para continuar",
                        color = Color(0xFF6D7A8F),
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LoginTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        placeholder = "Email",
                        icon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LoginTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        placeholder = "Contraseña",
                        icon = Icons.Outlined.Lock,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showPassword = !showPassword
                                }
                            ) {
                                Icon(
                                    imageVector = if (showPassword) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = "Mostrar contraseña",
                                    tint = Color(0xFF66758A)
                                )
                            }
                        }
                    )

                    errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = message,
                            color = Color(0xFFE53935),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    GradientLoginButton(
                        isLoading = isLoading,
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null

                                try {
                                    authRepository.login(
                                        email = email.trim(),
                                        password = password
                                    )

                                    onLoginSuccess()
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "No se pudo iniciar sesión."
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    )


                }
            }
        }
    }
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(15.dp),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF0078FF)
            )
        },
        trailingIcon = trailingIcon,
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF7B8497),
                fontSize = 16.sp
            )
        },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF0078FF),
            unfocusedBorderColor = Color(0xFFC8D2E0),
            focusedTextColor = Color(0xFF061B3A),
            unfocusedTextColor = Color(0xFF061B3A),
            cursorColor = Color(0xFF0078FF),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun GradientLoginButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF005BFF),
                        Color(0xFF00D9E8)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isLoading) {
                            listOf(
                                Color(0xFF6D7A8F),
                                Color(0xFF94A3B8)
                            )
                        } else {
                            listOf(
                                Color(0xFF005BFF),
                                Color(0xFF00D9E8)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ingresar",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Ingresar",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteBackground() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        val mapLineColor = Color(0xFF1B6FD1).copy(alpha = 0.14f)
        val routeColor = Color(0xFF00B8FF).copy(alpha = 0.48f)

        drawLine(
            color = mapLineColor,
            start = Offset(-80f, height * 0.26f),
            end = Offset(width * 0.55f, height * 0.08f),
            strokeWidth = 5f
        )

        drawLine(
            color = mapLineColor,
            start = Offset(width * 0.05f, height * 0.82f),
            end = Offset(width * 0.76f, height * 0.62f),
            strokeWidth = 4f
        )

        drawLine(
            color = mapLineColor,
            start = Offset(width * 0.18f, height),
            end = Offset(width * 0.95f, height * 0.74f),
            strokeWidth = 4f
        )

        drawLine(
            color = mapLineColor,
            start = Offset(-40f, height * 0.66f),
            end = Offset(width * 0.48f, height * 0.95f),
            strokeWidth = 4f
        )

        val routePath = Path().apply {
            moveTo(width * 0.75f, height * 0.08f)
            cubicTo(
                width * 0.58f,
                height * 0.22f,
                width * 1.05f,
                height * 0.18f,
                width * 0.86f,
                height * 0.36f
            )
            cubicTo(
                width * 0.70f,
                height * 0.50f,
                width * 0.55f,
                height * 0.63f,
                width * 0.84f,
                height * 0.82f
            )
        }

        drawPath(
            path = routePath,
            color = routeColor,
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(22f, 18f),
                    phase = 0f
                )
            )
        )

        drawLocationPin(
            center = Offset(width * 0.76f, height * 0.08f),
            color = Color(0xFF0078FF)
        )

        drawLocationPin(
            center = Offset(width * 0.92f, height * 0.18f),
            color = Color(0xFF00D9E8)
        )

        drawLocationPin(
            center = Offset(width * 0.86f, height * 0.88f),
            color = Color(0xFF00D9E8)
        )
    }
}

private fun DrawScope.drawLocationPin(
    center: Offset,
    color: Color
) {
    drawCircle(
        color = color.copy(alpha = 0.23f),
        radius = 25f,
        center = center
    )

    drawCircle(
        color = color,
        radius = 13f,
        center = center
    )

    drawCircle(
        color = Color(0xFF031225),
        radius = 5f,
        center = center
    )

    drawLine(
        color = color,
        start = Offset(center.x, center.y + 12f),
        end = Offset(center.x, center.y + 40f),
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )
}