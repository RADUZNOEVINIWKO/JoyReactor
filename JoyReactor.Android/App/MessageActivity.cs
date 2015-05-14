﻿using System.Collections.ObjectModel;
using Android.App;
using Android.OS;
using Android.Views;
using Android.Widget;
using GalaSoft.MvvmLight.Helpers;
using Humanizer;
using JoyReactor.Android.App.Base;
using JoyReactor.Android.Widget;
using JoyReactor.Core.Model.DTO;
using JoyReactor.Core.ViewModels;

namespace JoyReactor.Android.App
{
    [Activity(Label = "@string/activity_private_messages")]			
    public class MessageActivity : BaseActivity
    {
        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);
            SetContentView(Resource.Layout.activity_messages);

// TODO:
//            var panel = FindViewById<DrawerLayout÷>(Resource.Id.slidePanel); 
//            if (savedInstanceState == null)
//                panel.OpenPane();
//            MessengerInstance.Register<MessagesViewModel.SelectThreadMessage>(this, _ => panel.ClosePane());
        }

        #region Threads

        public class ThreadListFragment : BaseFragment
        {
            MessageThreadsViewModel viewmodel;

            public override void OnCreate(Bundle savedInstanceState)
            {
                base.OnCreate(savedInstanceState);
                RetainInstance = true;
                (viewmodel = new MessageThreadsViewModel()).Initialize();
            }

            public override void OnDestroy()
            {
                base.OnDestroy();
                viewmodel.Cleanup();
            }

            public override View OnCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
            {
                var view = inflater.Inflate(Resource.Layout.fragment_message_threads, null);
                var list = view.FindViewById<ListView>(Resource.Id.list);
                list.ItemClick += (sender, e) => viewmodel.SelectedIndex = e.Position;
                list.Adapter = new ObservableAdapter<MessageThreadItem>
                {
                    DataSource = viewmodel.Threads,
                    GetTemplateDelegate = (position, s, v) =>
                    {
                        v = v ?? View.Inflate(Activity, Resource.Layout.item_message_thread, null);
                        v.FindViewById<TextView>(Resource.Id.username).Text = s.UserName;
                        v.FindViewById<TextView>(Resource.Id.lastMessage).Text = s.LastMessage;
                        v.FindViewById<TextView>(Resource.Id.time).Text = s.LastMessageTime.Humanize();
                        v.FindViewById<WebImageView>(Resource.Id.userImage).ImageSource = s.UserImage;
                        return v;
                    },
                };
                var progress = view.FindViewById(Resource.Id.progress);
                var binding = viewmodel
					.SetBinding(() => viewmodel.IsBusy, progress, () => progress.Visibility)
					.ConvertSourceToTarget(s => s ? ViewStates.Visible : ViewStates.Gone);
                bindings.Add(binding);
                return view;
            }
        }

        #endregion

        #region Messages

        public class MessageListFragment : BaseFragment
        {
            MessagesViewModel viewmodel;

            public override void OnCreate(Bundle savedInstanceState)
            {
                base.OnCreate(savedInstanceState);
                RetainInstance = true;
                viewmodel = new MessagesViewModel();
            }

            public override void OnDestroy()
            {
                base.OnDestroy();
                viewmodel.Cleanup();
            }

            public override View OnCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
            {
                var view = inflater.Inflate(Resource.Layout.fragment_messages, null);
                var list = view.FindViewById<ListView>(Resource.Id.list);
                list.Adapter = new MessageAdapter(viewmodel.Messages);

                var newMessage = view.FindViewById<EditText>(Resource.Id.newMessage);
                Binding binding = viewmodel.SetBinding(() => viewmodel.NewMessage, newMessage, () => newMessage.Text, BindingMode.TwoWay);
                bindings.Add(binding);

                var progress = view.FindViewById(Resource.Id.progress);
                binding = viewmodel
                    .SetBinding(() => viewmodel.IsBusy, progress, () => progress.Visibility)
                    .ConvertSourceToTarget(s => s ? ViewStates.Visible : ViewStates.Gone);
                bindings.Add(binding);

                view.FindViewById(Resource.Id.createMessage).SetCommand("Click", viewmodel.CreateMessageCommand);
                return view;
            }

            class MessageAdapter : BaseAdapter<PrivateMessage>
            {
                ObservableCollection<PrivateMessage> dataSource { get; set; }

                internal MessageAdapter(ObservableCollection<PrivateMessage> dataSource)
                {
                    this.dataSource = dataSource;
                    dataSource.CollectionChanged += (sender, e) => NotifyDataSetChanged();
                }

                public override long GetItemId(int position)
                {
                    return position;
                }

                public override View GetView(int position, View convertView, ViewGroup parent)
                {
                    if (convertView == null)
                        convertView = CreateView(position);
                    var s = dataSource[position];
                    convertView.FindViewById<TextView>(Resource.Id.message).Text = s.Message;
                    convertView.FindViewById<TextView>(Resource.Id.created).Text = s.Created.Humanize();

                    string name;
                    if (IsInboxItem(position)) {
                        name = position > 0 && IsInboxItem(position - 1) ? "inbox" : "inbox_first";
                    } else {
                        name = position > 0 && !IsInboxItem(position - 1) ? "outbox" : "outbox_first";
                    }
                    convertView.FindViewById(Resource.Id.content).Background = VectorDrawable.NewVectorDrawable(name);

                    return convertView;
                }

                View CreateView(int position)
                {
                    var resId = IsInboxItem(position) 
                        ? Resource.Layout.item_message_inbox 
                        : Resource.Layout.item_message_outbox;
                    return View.Inflate(App.Instance, resId, null);
                }

                bool IsInboxItem(int position)
                {
                    return GetItemViewType(position) == 0;
                }

                public override int Count
                {
                    get { return dataSource.Count; }
                }

                public override PrivateMessage this [int index]
                {
                    get { throw new System.NotImplementedException(); }
                }

                public override int GetItemViewType(int position)
                {
                    return dataSource[position].Mode == PrivateMessage.ModeInbox ? 0 : 1;
                }

                public override int ViewTypeCount
                {
                    get { return 2; }
                }
            }
        }

        #endregion
    }
}