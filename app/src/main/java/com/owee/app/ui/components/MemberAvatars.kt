package com.owee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.owee.app.data.remote.model.User

@Composable
fun MemberAvatars(
    members: List<User>,
    maxVisible: Int = 3,
    avatarSize: Dp = 24.dp,
    overlap: Dp = 8.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = overlap / 2)
    ) {
        val visibleMembers = members.take(maxVisible)
        val remainingCount = members.size - maxVisible

        Box {
            visibleMembers.forEachIndexed { index, user ->
                Box(
                    modifier = Modifier
                        .padding(start = if (index == 0) 0.dp else (avatarSize - overlap) * index)
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    if (user.photo_url != null) {
                        AsyncImage(
                            model = user.photo_url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.name.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = (avatarSize.value * 0.4).sp
                            )
                        }
                    }
                }
            }

            if (remainingCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = (avatarSize - overlap) * visibleMembers.size)
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$remainingCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = (avatarSize.value * 0.4).sp
                    )
                }
            }
        }
    }
}
