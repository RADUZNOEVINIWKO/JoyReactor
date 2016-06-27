package y2k.joyreactor.services

import rx.Completable
import rx.Observable
import rx.Single
import y2k.joyreactor.common.*
import y2k.joyreactor.common.platform.Platform
import y2k.joyreactor.model.*
import y2k.joyreactor.services.repository.DataContext
import y2k.joyreactor.services.requests.ChangePostFavoriteRequest
import y2k.joyreactor.services.requests.LikePostRequest
import y2k.joyreactor.services.requests.OriginalImageRequestFactory
import y2k.joyreactor.services.requests.PostRequest
import java.io.File

/**
 * Created by y2k on 11/24/15.
 */
class PostService(
    private val imageRequestFactory: OriginalImageRequestFactory,
    private val postRequest: PostRequest,
    private val buffer: MemoryBuffer,
    private val dataContext: DataContext.Factory,
    private val likePostRequest: LikePostRequest,
    private val platform: Platform,
    private val broadcastService: BroadcastService,
    private val changePostFavoriteRequest: ChangePostFavoriteRequest) {

    fun toggleFavorite(postId: Long): Completable {
        return dataContext
            .applyUse { Posts.first("id" to postId) }
            .flatMap { changePostFavoriteRequest.execute(postId, !it.isFavorite) }
            .mapDatabase(dataContext) {
                Posts.updateAll("id" to postId) { it.copy(isFavorite = !it.isFavorite) }
                broadcastService.broadcast(Notifications.Posts)
            }
            .toCompletable()
    }

    fun getVideo(postId: String): Observable<File> {
        return getFromCache(postId)
            .map { it.image!!.fullUrl("mp4") }
            .flatMap { imageRequestFactory.request(it) }
    }

    fun synchronizePost(postId: Long): Completable {
        return ioObservable {
            val response = postRequest.request(postId.toString())
            buffer.updatePost(response)

            broadcastService.broadcast(Notifications.Post)
        }.flatMap {
            dataContext.applyUse { Posts.getById(postId.toLong()) }
        }.flatMap {
            imageRequestFactory.request(it.image!!.fullUrl(null))
        }.doOnCompleted {
            broadcastService.broadcast(Notifications.Post)
        }.toCompletable()
    }

    fun getPost(postId: Long): Single<Post> {
        return dataContext.applyUse { Posts.getById(postId) }.toSingle()
    }

    fun getTopComments(count: Int, postId: Long): Single<List<Comment>> {
        return RootComments.create(dataContext, postId)
            .map { it.filter { it.level == 0 }.sortedByDescending { it.rating }.take(10) }
            .toSingle()
    }

    fun getComments(postId: Long, parentCommentId: Long): Single<CommentGroup> {
        return when (parentCommentId) {
            0L -> RootComments.create(dataContext, postId)
            else -> ChildComments.create(dataContext, parentCommentId, postId)
        }.toSingle()
    }

    fun getCommentsAsync(postId: Long, parentCommentId: Long): Observable<CommentGroup> {
        return when (parentCommentId) {
            0L -> RootComments.create(dataContext, postId)
            else -> ChildComments.create(dataContext, parentCommentId, postId)
        }
    }

    fun getFromCache(postId: String): Observable<Post> {
        return dataContext.applyUse { Posts.getById(postId.toLong()) }
    }

    fun getImages(postId: Long): Single<List<Image>> {
        return dataContext
            .applyUse {
                val postAttachments = attachments
                    .filter("postId" to postId)
                    .mapNotNull { it.image }
                val commentAttachments = comments
                    .filter("postId" to postId)
                    .map { it.attachmentObject }
                    .filterNotNull()
                postAttachments.union(commentAttachments).toList()
            }
            .toSingle()
    }

    fun getPostImages(postId: Long): Single<List<Image>> {
        return dataContext
            .applyUse {
                val postAttachments = attachments
                    .filter("postId" to postId)
                    .mapNotNull { it.image }
                val commentAttachments = comments
                    .filter("postId" to postId)
                    .map { it.attachmentObject }
                    .filterNotNull()
                postAttachments.union(commentAttachments).toList()
            }
            .toSingle()
    }

    fun getSimilarPosts(postId: Long): Observable<List<SimilarPost>> {
        return dataContext
            .applyUse { similarPosts.filter("postId" to postId) }
    }

    fun saveImageToGallery(postId: Long): Completable {
        return getFromCache("" + postId)
            .flatMap { mainImage(it.id) }
            .flatMap { platform.saveToGallery(it) }
            .toCompletable()
    }

    fun mainImageFromDisk(serverPostId: Long): Single<PartialResult<File>> {
        return dataContext
            .applyUse { Posts.getById(serverPostId) }
            .flatMap { imageRequestFactory.requestFromCache(it.image!!.fullUrl(null)) }
            .map { PartialResult.complete(it) }
            .toSingle()
    }

    fun mainImage(serverPostId: Long): Observable<File> {
        return dataContext
            .applyUse { Posts.getById(serverPostId) }
            .flatMap { imageRequestFactory.request(it.image!!.fullUrl(null)) }
    }

    fun updatePostLike(postId: Long, like: Boolean): Completable {
        return likePostRequest
            .like(postId, like)
            .flatMap {
                dataContext.applyUse {
                    val post = Posts.getById(postId)
                    Posts.add(post.copy(rating = it.first, myLike = it.second))
                }
            }
            .doOnCompleted { BroadcastService.broadcast(Notifications.Posts) }
            .toCompletable()
    }
}