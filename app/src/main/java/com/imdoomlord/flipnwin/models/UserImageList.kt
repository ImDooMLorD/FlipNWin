package com.imdoomlord.flipnwin.models

import com.google.firebase.firestore.PropertyName

data class UserImageList(
    @PropertyName("images") var images: List<String>? = null

)
