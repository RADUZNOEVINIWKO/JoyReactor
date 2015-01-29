﻿using GalaSoft.MvvmLight;
using GalaSoft.MvvmLight.Command;
using JoyReactor.Core.Model;
using JoyReactor.Core.Model.DTO;
using JoyReactor.Core.Model.Helper;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Reactive.Linq;

namespace JoyReactor.Core.ViewModels
{
    public class PostViewModel : ViewModelBase
    {
        public ObservableCollection<ViewModelBase> ViewModelParts { get; } = new ObservableCollection<ViewModelBase>();

        bool _isBusy;
        public bool IsBusy
        {
            get { return _isBusy; }
            set { Set(ref _isBusy, value); }
        }

        public RelayCommand OpenGalleryCommand { get; set; }

        IPostService postService;
        ICommentService commentService;

        public PostViewModel()
        {
#if DEBUG
            if (IsInDesignMode)
            {

                var items = Enumerable
                    .Range(1, 10)
                    .Select(s => Enumerable.Range(0, 2 * s))
                    .Select(s => s.Select(_ => "Тестовый текст - "))
                    .Select(s => s.Aggregate((a, b) => a + b))
                    .Select(s => new CommentViewModel { Text = s });
                ViewModelParts.AddRange(items);
            }
#endif
        }

        public void Initialize(int postId)
        {
            postService = new PostService(postId);
            commentService = new CommentService(postId);

            postService
                .Get()
                .SubscribeOnUi(post =>
                {
                    var poster = post.Attachments.Select(s => s.PreviewImageUrl).FirstOrDefault();
                    ViewModelParts.ReplaceAt(0, new PosterViewModel { Image = poster });
                });
            ReloadCommentList(0);
        }

        private void ReloadCommentList(int commentId)
        {
            commentService
                .Get(commentId)
                .SubscribeOnUi(comments =>
                {
                    ViewModelParts.ReplaceAll(1, new ViewModelBase[0]);
                    if (comments.Count >= 2 && comments[0].Id == comments[1].ParentCommentId)
                    {
                        ViewModelParts.Insert(1, new CommentViewModel(this, comments[0]) { IsRoot = true });
                        comments.RemoveAt(0);
                    }
                    ViewModelParts.AddRange(ConvertToViewModels(comments));
                });
        }

        IEnumerable<CommentViewModel> ConvertToViewModels(IEnumerable<Comment> comments)
        {
            foreach (var s in comments)
                yield return new CommentViewModel(this, s);
        }

        public class PosterViewModel : ViewModelBase
        {
            string _image;
            public string Image
            {
                get { return _image; }
                set { Set(ref _image, value); }
            }
        }

        public class CommentViewModel : ViewModelBase
        {
            public RelayCommand NavigateCommand { get; set; }

            public bool IsRoot { get; set; }

            public string Text { get; set; }

            public int ChildCount { get; set; }

            public ICollection<string> Images { get; set; }

            public CommentViewModel() { }

            public CommentViewModel(PostViewModel parent, Comment comment)
            {
                Text = comment.Text;
                ChildCount = comment.ChildCount;
                NavigateCommand = new FixRelayCommand(() => parent.ReloadCommentList(IsRoot ? comment.ParentCommentId : comment.Id));
            }
        }

        internal interface IPostService
        {
            IObservable<Post> Get();
        }

        internal interface ICommentService
        {
            IObservable<List<Comment>> Get(int comment);
        }
    }
}