package y2k.joyreactor.viewmodel

import y2k.joyreactor.common.binding
import y2k.joyreactor.common.await
import y2k.joyreactor.platform.NavigationService
import y2k.joyreactor.services.PostService
import java.io.File

/**
 * Created by y2k on 3/8/16.
 */
class ImageViewModel(service: PostService) {

    val isBusy = binding(false)
    val imageFile = binding<File>()

    init {
        isBusy += true
        service
            .mainImage(NavigationService.instance.argument.toLong())
            .await({
                imageFile += it
                isBusy += false
            }) {
                it.printStackTrace()
                isBusy += false
            }
    }
}