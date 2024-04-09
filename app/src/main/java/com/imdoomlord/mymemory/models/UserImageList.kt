package com.imdoomlord.mymemory.models

import com.google.firebase.firestore.PropertyName

data class UserImageList(
    @PropertyName("images") var images: List<String>? = null

)
