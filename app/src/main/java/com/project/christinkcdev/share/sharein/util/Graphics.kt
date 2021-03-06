package com.project.christinkcdev.share.sharein.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.project.christinkcdev.share.sharein.GlideApp
import com.project.christinkcdev.share.sharein.R
import com.project.christinkcdev.share.sharein.data.ClientRepository
import com.project.christinkcdev.share.sharein.data.UserDataRepository
import com.project.christinkcdev.share.sharein.database.model.UClient
import com.project.christinkcdev.share.sharein.drawable.TextDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.monora.uprotocol.core.protocol.Client
import java.util.*

object Graphics {
    fun createIconBuilder(context: Context) = TextDrawable.createBuilder().apply {
        textFirstLetters = true
        textMaxLength = 2
        textBold = true
        textColor = R.attr.colorControlNormal
        shapeColor = R.attr.colorPassive
    }

    fun deleteLocalClientPicture(context: Context) {
        context.deleteFile(UserDataRepository.FILE_CLIENT_PICTURE)
        changeLocalClientPictureChecksum(context, 0)
    }

    fun saveClientPictureLocal(context: Context, uri: Uri) {
        GlideApp.with(context).load(uri)
            .centerCrop()
            .override(200, 200)
            .into(LocalPictureTarget(context))
    }

    suspend fun saveClientPicture(
        context: Context,
        clientRepository: ClientRepository,
        client: Client,
        data: ByteArray?,
        checksum: Int,
    ) {
        if (client !is UClient) throw UnsupportedOperationException()

        if (data == null) {
            clientRepository.update(
                client.also {
                    it.pictureFile?.delete()
                    it.pictureFile = null
                    it.checksum = checksum
                }
            )
        }

        val path = UUID.randomUUID().toString()

        GlideApp.with(context)
            .load(data)
            .centerCrop()
            .override(200, 200)
            .into(PictureTarget(context, clientRepository, client, checksum, path))
    }
}

private fun changeLocalClientPictureChecksum(context: Context, checksum: Int) {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
        putInt(UserDataRepository.KEY_PICTURE_CHECKSUM, checksum)
    }
}

private fun processNewPicture(context: Context, path: String, resource: Drawable): Boolean {
    if (resource !is BitmapDrawable) throw IllegalStateException()

    try {
        context.openFileOutput(path, Context.MODE_PRIVATE).use { outputStream ->
            resource.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        return true
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return false
}

private class PictureTarget(
    private val context: Context,
    private val clientRepository: ClientRepository,
    private val client: UClient,
    private val checksum: Int,
    private val path: String,
) : CustomTarget<Drawable>() {
    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        if (processNewPicture(context, path, resource)) {
            GlobalScope.launch(Dispatchers.IO) {
                clientRepository.update(
                    client.also {
                        it.pictureFile = context.getFileStreamPath(path)
                        it.checksum = checksum
                    }
                )
            }
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {}
}

private class LocalPictureTarget(private val context: Context) : CustomTarget<Drawable>() {
    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        if (processNewPicture(context, UserDataRepository.FILE_CLIENT_PICTURE, resource)) {
            GlobalScope.launch(Dispatchers.IO) {
                context.openFileInput(UserDataRepository.FILE_CLIENT_PICTURE).use {
                    changeLocalClientPictureChecksum(context, it.readBytes().contentHashCode())
                }
            }
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {}
}